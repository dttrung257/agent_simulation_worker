package com.uet.agent_simulation_worker.services.experiment_result_image;

import com.uet.agent_simulation_worker.constant.AppConst;
import com.uet.agent_simulation_worker.exceptions.errors.ExperimentResultErrors;
import com.uet.agent_simulation_worker.exceptions.errors.ExperimentResultImageErrors;
import com.uet.agent_simulation_worker.exceptions.experiment_result.ExperimentResultNotFoundException;
import com.uet.agent_simulation_worker.exceptions.experiment_result_image.ExperimentResultImageDataReadException;
import com.uet.agent_simulation_worker.exceptions.experiment_result_image.ExperimentResultImageNotFoundException;
import com.uet.agent_simulation_worker.exceptions.node.CannotFetchNodeDataException;
import com.uet.agent_simulation_worker.models.ExperimentResultImage;
import com.uet.agent_simulation_worker.models.projections.ExperimentResultImageDetailProjection;
import com.uet.agent_simulation_worker.repositories.ExperimentResultImageRepository;
import com.uet.agent_simulation_worker.repositories.ExperimentResultRepository;
import com.uet.agent_simulation_worker.responses.Pagination;
import com.uet.agent_simulation_worker.responses.SuccessResponse;
import com.uet.agent_simulation_worker.responses.experiment_result_image.ExperimentResultImageCategoryResponse;
import com.uet.agent_simulation_worker.responses.experiment_result_image.ExperimentResultImageDetailResponse;
import com.uet.agent_simulation_worker.responses.experiment_result_image.ExperimentResultImageListResponse;
import com.uet.agent_simulation_worker.responses.experiment_result_image.ExperimentResultImageStepResponse;
import com.uet.agent_simulation_worker.services.auth.IAuthService;
import com.uet.agent_simulation_worker.services.image.IImageService;
import com.uet.agent_simulation_worker.services.node.INodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExperimentResultImageService implements IExperimentResultImageService {
    private final IAuthService authService;
    private final INodeService nodeService;
    private final IImageService imageService;
    private final ExperimentResultRepository experimentResultRepository;
    private final ExperimentResultImageRepository experimentResultImageRepository;

    @Override
    public Pagination<List<ExperimentResultImage>> get(BigInteger experimentResultId, BigInteger experimentId,
            BigInteger modelId, BigInteger projectId, BigInteger experimentResultCategoryId, Integer step, Integer page,
            Integer pageSize, String orderBy, String orderDirection) {
        final var sortFields = AppConst.EXPERIMENT_RESULT_IMAGE_SORT_FIELDS_NATIVE;
        final var sort = createSort(orderBy, orderDirection, sortFields);
        final var pageable = createPageRequest(page, pageSize, sort);

        final var experimentResultImagePage = experimentResultImageRepository.find(authService.getCurrentUserId(),
            experimentResultId, experimentId, modelId, projectId, experimentResultCategoryId, step, pageable);

        return new Pagination<>(BigInteger.valueOf(experimentResultImagePage.getTotalElements()),
                experimentResultImagePage.getContent());
    }

    private Sort createSort(String orderBy, String orderDirection, List<String> sortFields) {
        if (StringUtils.hasText(orderBy) && StringUtils.hasText(orderDirection) &&
                sortFields.contains(orderBy) && AppConst.ORDER_DIRECTIONS.contains(orderDirection)) {
            return Sort.by(Sort.Direction.valueOf(orderDirection.toUpperCase()), orderBy);
        }

        return Sort.unsorted();
    }

    private PageRequest createPageRequest(Integer page, Integer pageSize, Sort sort) {
        pageSize = pageSize != null && pageSize > AppConst.PAGE_MAX_SIZE ? AppConst.PAGE_MAX_SIZE : pageSize;

        return PageRequest.of(page - 1, pageSize, sort);
    }

    /**
     * Get image data.
     *
     * @param id BigInteger
     *
     * @return ExperimentResultImageDetailResponse
     */
    @Override
    public ExperimentResultImageDetailResponse getImageData(BigInteger id) {
        final var experimentResultImageDetailProjection = experimentResultImageRepository.findDetailById(id, authService.getCurrentUserId())
                .orElseThrow(() -> new ExperimentResultImageNotFoundException(ExperimentResultImageErrors.E_ERI_0001.defaultMessage()));

        // Check if image data is valid.
        checkImageData(experimentResultImageDetailProjection);

        // If image is in current node, return image data.
        if (isCurrentNodeImage(nodeService.getCurrentNodeId(), experimentResultImageDetailProjection.getNodeId())) {
            return getCurrentNodeImage(experimentResultImageDetailProjection);
        }

        // Fetch image from node.
        final var mediaType = imageService.getImageMediaType(experimentResultImageDetailProjection.getExtension());
        final var base64EncodedImage = getImageFromNode(experimentResultImageDetailProjection.getId(),
                experimentResultImageDetailProjection.getNodeId());

        return new ExperimentResultImageDetailResponse(mediaType, decodeImageData(base64EncodedImage));
    }

    /**
     * Ensure image data is valid.
     * node_id, location, extension must not be null.
     *
     * @param experimentResultImageDetailProjection ExperimentResultImageDetailProjection
     */
    private void checkImageData(ExperimentResultImageDetailProjection experimentResultImageDetailProjection) {
        final var isValid = experimentResultImageDetailProjection != null
                && experimentResultImageDetailProjection.getNodeId() != null
                && experimentResultImageDetailProjection.getLocation() != null
                && experimentResultImageDetailProjection.getExtension() != null;

        if (!isValid) {
            throw new ExperimentResultImageNotFoundException(ExperimentResultImageErrors.E_ERI_0001.defaultMessage());
        }
    }

    /**
     * Check if image is in current node.
     *
     * @param currentNodeId Integer
     * @param imageNodeId Integer
     *
     * @return Boolean
     */
    private Boolean isCurrentNodeImage(Integer currentNodeId, Integer imageNodeId) {
        return currentNodeId.equals(imageNodeId);
    }

    /**
     * Get image from current node.
     *
     * @param experimentResultImageDetailProjection ExperimentResultImageDetailProjection
     *
     * @return mediaType, imageData in byte[]
     */
    private ExperimentResultImageDetailResponse getCurrentNodeImage(
            ExperimentResultImageDetailProjection experimentResultImageDetailProjection) {
        final var filePath = Paths.get(experimentResultImageDetailProjection.getLocation());
        if (!Files.exists((filePath))) {
            throw new ExperimentResultImageNotFoundException("This image does no longer exist in the server.");
        }

        final var imageData = readImage(filePath);
        final var mediaType = imageService.getImageMediaType(experimentResultImageDetailProjection.getExtension());

        return new ExperimentResultImageDetailResponse(mediaType, imageData);
    }

    /**
     * Read image data from file.
     *
     * @param filePath Path
     *
     * @return byte[]
     */
    private byte[] readImage(Path filePath) {
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new ExperimentResultImageDataReadException(ExperimentResultImageErrors.E_ERI_0002.defaultMessage());
        }
    }

    /**
     * Create webClient to fetch image from node.
     * Then fetch image from node.
     *
     * @param imageId BigInteger
     * @param nodeId Integer
     *
     * @return base64 encoded image
     */
    private String getImageFromNode(BigInteger imageId, Integer nodeId) {
        final var webClient = nodeService.getWebClientByNodeId(nodeId);

        return fetchImage(webClient, imageId);
    }

    /**
     * Fetch image from node.
     *
     * @param webClient WebClient
     * @param imageId BigInteger
     *
     * @return base64 encoded image
     */
    private String fetchImage(WebClient webClient, BigInteger imageId) {
        try {
            final var response = webClient.get().uri("/api/v1/experiment_result_images/" + imageId + "/encode")
                    .retrieve().bodyToMono(String.class).block();

            if (response == null) {
                throw new ExperimentResultImageNotFoundException(ExperimentResultImageErrors.E_ERI_0001.defaultMessage());
            }

            return response;
        } catch (Exception e) {
            log.error("Error while fetching image: {}", e.getMessage());

            throw new CannotFetchNodeDataException(e.getMessage());
        }
    }

    /**
     * After fetching image data from node, decode it.
     *
     * @param base64EncodedImage String
     *
     * @return byte[]
     */
    private byte[] decodeImageData(String base64EncodedImage) {
        try {
            return Base64.getDecoder().decode(base64EncodedImage);
        } catch (Exception e) {
            log.error("Error while decoding image data: {}", e.getMessage());

            throw new CannotFetchNodeDataException("Cannot decode image data from node.");
        }
    }

    @Override
    public String getImageDataEncoded(BigInteger id) {
        final var image = getImageData(id);

        return Base64.getEncoder().encodeToString(image.data());
    }

    @Override
    public ExperimentResultImageListResponse getByRange(BigInteger experimentResultId, Integer startStep, Integer endStep) {
        final var experimentResult = experimentResultRepository.findById(experimentResultId)
                .orElseThrow(() -> new ExperimentResultNotFoundException(ExperimentResultErrors.E_ER_0001.defaultMessage()));

        if (!nodeService.getCurrentNodeId().equals(experimentResult.getNodeId())) {
            return getExperimentResultImageFromNode(experimentResultId, startStep, endStep, experimentResult.getNodeId());
        }

        final var imageData = experimentResultImageRepository.findByRange(experimentResultId, startStep, endStep,
                authService.getCurrentUserId());

        final var steps = imageData.stream().map(ExperimentResultImage::getStep).distinct().toList();
        final var stepsData = new ArrayList<ExperimentResultImageStepResponse>();
        final var seen = new HashSet<>();
        steps.forEach(step -> {
            imageData.forEach(image -> {
                if (image.getStep().equals(step) && !seen.contains(step)) {
                    seen.add(step);
                    stepsData.add(new ExperimentResultImageStepResponse(step, new ArrayList<>()));
                }
            });
        });

        stepsData.forEach(stepData -> {
            imageData.forEach(image -> {
                if (image.getStep().equals(stepData.step())) {
                    final var base64EncodedImage = imageService.getImageDataEncoded(image.getLocation());
                    stepData.categories().add(new ExperimentResultImageCategoryResponse(
                            image.getExperimentResultCategoryId(), base64EncodedImage));
                }
            });
        });

        return new ExperimentResultImageListResponse(stepsData);
    }

    private ExperimentResultImageListResponse getExperimentResultImageFromNode(BigInteger experimentResultId, Integer startStep, Integer endStep, Integer nodeId) {
        final var webClient = nodeService.getWebClientByNodeId(nodeId);

        return fetchExperimentResultImageFromNode(webClient, experimentResultId, startStep, endStep);
    }

    private ExperimentResultImageListResponse fetchExperimentResultImageFromNode(WebClient webClient, BigInteger experimentResultId, Integer startStep, Integer endStep) {
        try {
            final var response = webClient.get().uri("/api/v1/experiment_result_images?experiment_result_id=" + experimentResultId +
                    "&start_step=" + startStep + "&end_step=" + endStep).retrieve().bodyToMono(ExperimentResultImageListResponse.class).block();

            if (response == null) {
                throw new ExperimentResultImageNotFoundException(ExperimentResultImageErrors.E_ERI_0001.defaultMessage());
            }

            return response;
        } catch (Exception e) {
            log.error("Error while fetching experiment result image: {}", e.getMessage());

            throw new CannotFetchNodeDataException(e.getMessage());
        }
    }
}

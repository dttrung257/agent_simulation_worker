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
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

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
                    stepData.categories().add(new ExperimentResultImageCategoryResponse(experimentResultId, image.getExperimentResultCategoryId(), base64EncodedImage));
                }
            });
        });

        return new ExperimentResultImageListResponse(stepsData);
    }

    private ExperimentResultImageListResponse getExperimentResultImageFromNode(BigInteger experimentResultId, Integer startStep, Integer endStep, Integer nodeId) {
        final var webClient = nodeService.getWebClientByNodeId(nodeId);

        try {
            final var response = webClient.get().uri(
                uriBuilder -> uriBuilder.path("/api/v1/experiment_result_images")
                    .queryParam("experiment_result_id", experimentResultId)
                    .queryParam("start_step", startStep)
                    .queryParam("end_step", endStep)
                    .build()
            ).retrieve().bodyToMono(ExperimentResultImageListResponse.class).block();

            if (response == null) {
                throw new ExperimentResultImageNotFoundException(ExperimentResultImageErrors.E_ERI_0001.defaultMessage());
            }

            return response;
        } catch (Exception e) {
            log.error("Error while fetching experiment result image from node: {}", e.getMessage());

            throw new CannotFetchNodeDataException(e.getMessage());
        }
    }


    @Override
    public Flux<ExperimentResultImageListResponse> getAnimatedImages(BigInteger experimentResultId, Integer startStep,
         Integer endStep, long duration) {
        final int CHUNK_SIZE = 10;

        final var experimentResult = experimentResultRepository.findById(experimentResultId)
            .orElseThrow(() -> new ExperimentResultNotFoundException(ExperimentResultErrors.E_ER_0001.defaultMessage()));

        if (!nodeService.getCurrentNodeId().equals(experimentResult.getNodeId())) {
            return getAnimatedImagesFromNode(experimentResultId, startStep, endStep, duration, experimentResult.getNodeId());
        }

        List<Pair<Integer, Integer>> chunks = new ArrayList<>();
        for (int i = startStep; i <= endStep; i += CHUNK_SIZE) {
            int chunkEnd = Math.min(i + CHUNK_SIZE - 1, endStep);
            chunks.add(Pair.of(i, chunkEnd));
        }

        return Flux.fromIterable(chunks)
                .concatMap(chunk -> {
                    final var images = experimentResultImageRepository.findByRange(
                        experimentResultId,
                        chunk.getFirst(),
                        chunk.getSecond(),
                        authService.getCurrentUserId()
                    );

                    final var groupedImages = images.stream()
                        .collect(Collectors.groupingBy(ExperimentResultImage::getStep));

                    return Flux.interval(Duration.ofMillis(duration))
                        .take(chunk.getSecond() - chunk.getFirst() + 1)
                        .flatMap(i -> {
                            var step = i.intValue() + chunk.getFirst();
                            var stepImages = groupedImages.get(step);
                            if (stepImages == null) {
                                return Flux.empty();
                            }

                            var categories = stepImages.stream()
                                    .map(image -> {
                                        var base64EncodedImage = imageService.getImageDataEncoded(image.getLocation());
                                        return new ExperimentResultImageCategoryResponse(experimentResultId,
                                                image.getExperimentResultCategoryId(), base64EncodedImage);
                                    })
                                    .collect(Collectors.toList());

                            return Flux.just(new ExperimentResultImageListResponse(
                                    List.of(new ExperimentResultImageStepResponse(step, categories))));
                        });
                });
    }

    @Override
    public Flux<ExperimentResultImageListResponse> getMultiExperimentAnimatedImages(String experimentResultIds, Integer startStep,
        Integer endStep, long duration) {
        final int CHUNK_SIZE = 10;

        List<BigInteger> experimentResultIdList = Arrays.stream(experimentResultIds.split(","))
                .map(BigInteger::new)
                .collect(Collectors.toList());

        List<Pair<Integer, Integer>> chunks = new ArrayList<>();
        for (int i = startStep; i <= endStep; i += CHUNK_SIZE) {
            int chunkEnd = Math.min(i + CHUNK_SIZE - 1, endStep);
            chunks.add(Pair.of(i, chunkEnd));
        }

        return Flux.fromIterable(chunks)
            .concatMap(chunk ->
                Flux.interval(Duration.ofNanos(duration))
                    .take(chunk.getSecond() - chunk.getFirst() + 1)
                    .flatMap(i -> {
                        int step = i.intValue() + chunk.getFirst();

                        return Flux.fromIterable(experimentResultIdList)
                            .flatMap(experimentResultId -> {
                                final var experimentResult = experimentResultRepository.findById(experimentResultId)
                                    .orElseThrow(() -> new ExperimentResultNotFoundException(ExperimentResultErrors.E_ER_0001.defaultMessage()));

                                if (!nodeService.getCurrentNodeId().equals(experimentResult.getNodeId())) {
                                    return getMultiExperimentAnimatedImagesFromNode(experimentResultId, step, step, duration, experimentResult.getNodeId());
                                }

                                final var images = experimentResultImageRepository.findByRange(experimentResultId, step, step,
                                    authService.getCurrentUserId());

                                if (images.isEmpty()) {
                                    return Flux.empty();
                                }

                                final var categories = images.stream()
                                    .map(image -> {
                                        var base64EncodedImage = imageService.getImageDataEncoded(image.getLocation());
                                        return new ExperimentResultImageCategoryResponse(experimentResultId,
                                                image.getExperimentResultCategoryId(), base64EncodedImage);
                                    })
                                    .collect(Collectors.toList());

                                return Flux.just(new ExperimentResultImageListResponse(
                                    List.of(new ExperimentResultImageStepResponse(step, categories))));
                            })
                            .collectList()
                            .flatMapMany(responses -> {
                                // Merge all categories from different experimentResults into one response
                                List<ExperimentResultImageStepResponse> mergedStepResponses = new ArrayList<>();
                                List<ExperimentResultImageCategoryResponse> allCategories = new ArrayList<>();

                                for(ExperimentResultImageListResponse response : responses) {
                                    if(!response.steps().isEmpty()) {
                                        allCategories.addAll(response.steps().getFirst().categories());
                                    }
                                }

                                if(!allCategories.isEmpty()) {
                                    mergedStepResponses.add(new ExperimentResultImageStepResponse(step, allCategories));
                                    return Flux.just(new ExperimentResultImageListResponse(mergedStepResponses));
                                }

                                return Flux.empty();
                            });
                    })
            );
    }

    private Flux<ExperimentResultImageListResponse> getAnimatedImagesFromNode(BigInteger experimentResultId, Integer startStep,
            Integer endStep, long duration, Integer nodeId) {
        final var webClient = nodeService.getWebClientByNodeId(nodeId);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/experiment_result_images/animation")
                    .queryParam("experiment_result_id", experimentResultId)
                    .queryParam("start_step", startStep)
                    .queryParam("end_step", endStep)
                    .queryParam("animation", true)
                    .queryParam("duration", duration)
                    .build())
                .retrieve()
                .bodyToFlux(ExperimentResultImageListResponse.class);
    }

    private Flux<ExperimentResultImageListResponse> getMultiExperimentAnimatedImagesFromNode(BigInteger experimentResultId, Integer startStep,
            Integer endStep, long duration, Integer nodeId) {
        final var webClient = nodeService.getWebClientByNodeId(nodeId);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/experiment_result_images/multi_experiment_animation")
                    .queryParam("experiment_result_id", experimentResultId)
                    .queryParam("start_step", startStep)
                    .queryParam("end_step", endStep)
                    .queryParam("animation", true)
                    .queryParam("duration", duration)
                    .build())
                .retrieve()
                .bodyToFlux(ExperimentResultImageListResponse.class)
                .onErrorResume(throwable -> {
                    log.error("Error fetching animated images from node {}: {}", nodeId, throwable.getMessage());

                    return Flux.just(new ExperimentResultImageListResponse(new ArrayList<>()));
                });
    }
}

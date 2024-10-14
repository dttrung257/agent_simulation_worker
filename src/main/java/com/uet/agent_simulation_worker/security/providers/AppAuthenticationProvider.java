package com.uet.agent_simulation_worker.security.providers;

import com.uet.agent_simulation_worker.exceptions.errors.CommonErrors;
import com.uet.agent_simulation_worker.models.AppUser;
import com.uet.agent_simulation_worker.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigInteger;

@Service
@RequiredArgsConstructor
public class AppAuthenticationProvider implements AuthenticationProvider {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        final BigInteger id = (BigInteger) authentication.getPrincipal();
        final String password = authentication.getCredentials().toString();

        final AppUser user = userRepository.findById(id).orElseThrow(
                () -> new UsernameNotFoundException(CommonErrors.E0005.defaultMessage()));

        if (passwordEncoder.matches(password, user.getPassword()))
            return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        throw new BadCredentialsException(CommonErrors.E0005.defaultMessage());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.equals(authentication);
    }
}

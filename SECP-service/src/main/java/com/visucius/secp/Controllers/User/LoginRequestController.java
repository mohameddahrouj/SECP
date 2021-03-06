package com.visucius.secp.Controllers.User;

import com.visucius.secp.Controllers.TokenController;
import com.visucius.secp.DTO.LoginRequestDTO;
import com.visucius.secp.DTO.TokenDTO;
import com.visucius.secp.daos.UserDAO;
import com.visucius.secp.models.User;
import com.visucius.secp.util.PasswordUtil;
import org.apache.commons.lang3.StringUtils;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class LoginRequestController {

    private final static Logger LOG = LoggerFactory.getLogger(LoginRequestController.class);
    private UserDAO userDAO;
    private TokenController tokenController;


    public LoginRequestController(TokenController tokenController, UserDAO userDAO) {
        this.userDAO = userDAO;
        this.tokenController = tokenController;
    }

    public Response login(LoginRequestDTO loginRequestDTO) {
        validate(loginRequestDTO);

        User user = userDAO.findByUserName(loginRequestDTO.getUsername());

        if (user == null) {
            LOG.warn("User not found.");
            throw new WebApplicationException(UserErrorMessage.LOGIN_FAIL_USER_NOT_FOUND, Response.Status.UNAUTHORIZED);
        }

        if(!user.isActive())
        {
            LOG.warn("User is not active.");
            throw new WebApplicationException(UserErrorMessage.LOGIN_USER_IS_NOT_ACTIVE, Response.Status.UNAUTHORIZED);
        }

        boolean isValidPassword = isPasswordValid(loginRequestDTO, user);

        if (isValidPassword) {
            String token = getToken(loginRequestDTO);

            TokenDTO tokenDTO = new TokenDTO(user.getId(), user.getUsername(), token, user.getLoginRole());
            return Response.ok().entity(tokenDTO).build();
        } else {
            throw new WebApplicationException(UserErrorMessage.LOGIN_FAIL_WRONG_PASSWORD, Response.Status.UNAUTHORIZED);
        }
    }

    private boolean isPasswordValid(LoginRequestDTO loginRequestDTO, User user) {
        boolean isPasswordValid;

        try {
            isPasswordValid = PasswordUtil.verifyPassword(loginRequestDTO.getPassword(), user.getPassword());
        } catch (PasswordUtil.CannotPerformOperationException e) {
            LOG.error("Unable to compute hash.", e);
            throw new WebApplicationException(UserErrorMessage.LOGIN_FAIL_WRONG_PASSWORD, Response.Status.UNAUTHORIZED);
        } catch (PasswordUtil.InvalidHashException e) {
            LOG.warn("Unable to compute hash. ", e);
            throw new WebApplicationException(UserErrorMessage.LOGIN_FAIL_WRONG_PASSWORD, Response.Status.UNAUTHORIZED);
        }

        return isPasswordValid;
    }

    private String getToken(LoginRequestDTO loginRequestDTO) {
        String token;

        try {
            token = tokenController.createTokenFromUsername(loginRequestDTO.getUsername());
        } catch (JoseException e) {
            LOG.error("Unable to create authToken.", e);
            throw new WebApplicationException(UserErrorMessage.LOGIN_FAIL_USER_NOT_FOUND, Response.Status.UNAUTHORIZED);
        }

        return token;
    }

    private void validate(LoginRequestDTO loginRequestDTO) {
        if (loginRequestDTO == null) {
            throw new WebApplicationException(UserErrorMessage.LOGIN_FAIL_NO_CREDENTIALS, Response.Status.BAD_REQUEST);
        }
        if (StringUtils.isBlank(loginRequestDTO.getUsername())) {
            throw new WebApplicationException(UserErrorMessage.LOGIN_FAIL_NO_USERNAME, Response.Status.BAD_REQUEST);
        } else if (StringUtils.isBlank(loginRequestDTO.getPassword())) {
            throw new WebApplicationException(UserErrorMessage.LOGIN_FAIL_NO_PASSWORD, Response.Status.BAD_REQUEST);
        }
    }
}

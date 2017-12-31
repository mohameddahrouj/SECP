package com.visucius.secp.resources;

import com.codahale.metrics.annotation.Timed;
import com.visucius.secp.Controllers.Admin.AdminController;
import com.visucius.secp.Controllers.User.UserRegistrationController;
import com.visucius.secp.DTO.UserRegistrationRequest;
import com.visucius.secp.DTO.UserRegistrationResponse;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RolesAllowed("ADMIN")
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminResource {
    private final AdminController adminController;
    private final UserRegistrationController userRegistrationController;


    public AdminResource(AdminController adminController, UserRegistrationController userRegistrationController) {
        this.adminController = adminController;
        this.userRegistrationController = userRegistrationController;
    }

    @POST
    @Path("{id}")
    @UnitOfWork
    public Response registerAdmin(@Auth @PathParam("id") String id) {
        adminController.registerAdmin(id);
        return Response.status(Response.Status.OK).build();
    }

    @DELETE
    @Path("{id}")
    @UnitOfWork
    public Response removeAdmin(@Auth @PathParam("id") String id) {
        adminController.removeAdmin(id);
        return Response.status(Response.Status.OK).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    @UnitOfWork
    @Path("/register")
    public Response create(@Auth UserRegistrationRequest request) {

        UserRegistrationResponse response = userRegistrationController.registerUser(request);

        if (response.success) {
            return Response.status(response.status).entity(response.toString()).build();
        }

        throw new WebApplicationException(response.getErrors(), response.status);
    }
}

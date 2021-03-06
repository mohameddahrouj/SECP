package com.visucius.secp.resources;


import com.codahale.metrics.annotation.Timed;
import com.visucius.secp.Controllers.FilterController;
import com.visucius.secp.DTO.FilterCreateRequest;
import com.visucius.secp.DTO.FilterDTO;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/filter")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"NORMAL", "ADMIN"})
public class FilterResource {

    private FilterController filterController;

    public FilterResource(FilterController controller)
    {
        this.filterController = controller;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    @UnitOfWork
    public Response create(@Auth FilterCreateRequest request) {
        return filterController.updateOrCreateFilter(request);
    }

    @GET
    @Path("/id/{id}")
    @UnitOfWork
    public Response getFilter(@Auth @PathParam("id") String id) {
        return filterController.getFilterGivenId(id);
    }

    @DELETE
    @UnitOfWork
    @Path("/{id}")
    public Response deleteFilter(@Auth @PathParam("id") int id)
    {
        return  filterController.deleteFilter(id);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timed
    @UnitOfWork
    @Path("/modify")
    public Response modifyGroup(@Auth FilterDTO request)
    {
        return  filterController.modifyFilter(request);
    }

    @GET
    @UnitOfWork
    public Response getAllFilters() {
        return filterController.getAllFilters();
    }

}

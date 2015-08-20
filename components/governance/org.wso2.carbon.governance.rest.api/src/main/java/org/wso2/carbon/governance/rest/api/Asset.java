/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.governance.rest.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.DetachedGenericArtifact;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceArtifactConfiguration;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.governance.rest.api.internal.PaginationInfo;
import org.wso2.carbon.governance.rest.api.model.AssetState;
import org.wso2.carbon.governance.rest.api.model.LCStateChange;
import org.wso2.carbon.governance.rest.api.model.TypedList;
import org.wso2.carbon.governance.rest.api.util.RESTUtil;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.pagination.PaginationContext;
import org.wso2.carbon.registry.core.service.RegistryService;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//TODO - test this
//@RolesAllowed("GOV-REST")
public class Asset {


    public static final String ENDPOINTS = "endpoints";
    private final Log log = LogFactory.getLog(Asset.class);

    @GET
    @Path("/types")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTypes() throws RegistryException {
        return getAssetTypes();
    }

    @GET
    @Path("{assetType : [a-zA-Z][a-zA-Z_0-9]*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAssets(@PathParam("assetType") String assetType, @Context UriInfo uriInfo)
            throws RegistryException {
        return getGovernanceAssets(assetType, uriInfo);
    }

    @GET
    @Path("{assetType : [a-zA-Z][a-zA-Z_0-9]*}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAsset(@PathParam("assetType") String assetType, @PathParam("id") String id)
            throws RegistryException {
        return getGovernanceAsset(assetType, id);
    }

    @POST
    @Path("{assetType : [a-zA-Z][a-zA-Z_0-9]*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAsset(@PathParam("assetType") String assetType, GenericArtifact genericArtifact,
                                @Context UriInfo uriInfo) throws RegistryException {
        return persistGovernanceAsset(assetType, (DetachedGenericArtifact) genericArtifact, RESTUtil.getBaseURL(uriInfo));
    }



    @PUT
    @Path("{assetType : [a-zA-Z][a-zA-Z_0-9]*}/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyAsset(@PathParam("assetType") String assetType, @PathParam("id") String id,
                                GenericArtifact genericArtifact, @Context UriInfo uriInfo) throws RegistryException {
        return modifyGovernanceAsset(assetType, id, (DetachedGenericArtifact) genericArtifact, RESTUtil.getBaseURL(uriInfo));
    }


    @DELETE
    @Path("{assetType : [a-zA-Z][a-zA-Z_0-9]*}/{id}")
    public Response deleteAsset(@PathParam("assetType") String assetType, @PathParam("id") String id)
            throws RegistryException {
        return deleteGovernanceAsset(assetType, id);
    }


    @GET
    @Path("/endpoints")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEndpoints(@Context UriInfo uriInfo) throws RegistryException {
        return getGovernanceAssets(ENDPOINTS, uriInfo);
    }

    @GET
    @Path("/endpoints/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEndpoint(@PathParam("id") String id) throws RegistryException {
        return getGovernanceAsset("endpoints", id);
    }


    @POST
    @Path("/endpoints")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createEndpoints(GenericArtifact genericArtifact, @Context UriInfo uriInfo)
            throws RegistryException {
        return persistGovernanceAsset(ENDPOINTS, (DetachedGenericArtifact) genericArtifact, RESTUtil.getBaseURL(uriInfo));
    }

    @PUT
    @Path("endpoints/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyEndpoint(@PathParam("id") String id,
                                   GenericArtifact genericArtifact, @Context UriInfo uriInfo) throws RegistryException {
        return modifyGovernanceAsset(ENDPOINTS, id, (DetachedGenericArtifact) genericArtifact, RESTUtil.getBaseURL(uriInfo));
    }


    @DELETE
    @Path("{endpoints/{id}")
    public Response deleteEndpoint(@PathParam("id") String id) throws RegistryException {
        return deleteGovernanceAsset("endpoints", id);
    }

    @GET
    @Path("{endpoint/{id}/states")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEndpointStates(@PathParam("id") String id,
                                      @Context UriInfo uriInfo) throws RegistryException {
        String lc = uriInfo.getQueryParameters().getFirst("lc");
        return getGovernanceAssetStates(ENDPOINTS, id, lc);
    }

    @GET
    @Path("{assetType : [a-zA-Z][a-zA-Z_0-9]*}/{id}/states")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAssetStates(@PathParam("assetType") String assetType, @PathParam("id") String id,
                                   @Context UriInfo uriInfo) throws RegistryException {
        String lc = uriInfo.getQueryParameters().getFirst("lc");
        return getGovernanceAssetStates(assetType, id, lc);
    }


    @PUT
    @Path("{assetType : [a-zA-Z][a-zA-Z_0-9]*}/{id}/states")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateLCState(@PathParam("assetType") String assetType, @PathParam("id") String id,
                                  LCStateChange stateChange,
                                  @Context UriInfo uriInfo) throws RegistryException {
        return updateLCState(assetType, id, stateChange);
    }


    private Response getAssetTypes() throws RegistryException {
        List<String> shortNames = new ArrayList<>();
        List<GovernanceArtifactConfiguration> configurations = GovernanceUtils.findGovernanceArtifactConfigurations
                (getUserRegistry());
        for (GovernanceArtifactConfiguration configuration : configurations) {
            shortNames.add(configuration.getSingularLabel());
        }
        return Response.ok().entity(shortNames).build();
    }


    private String createQuery(UriInfo uriInfo) {
        StringBuilder builder = new StringBuilder("");
        MultivaluedMap<String, String> queryParam = uriInfo.getQueryParameters();
        RESTUtil.excludePaginationParameters(queryParam);
        Iterator<Map.Entry<String, List<String>>> iterator = queryParam.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<String>> entry = iterator.next();
            String value = entry.getValue().get(0);
            if (value != null) {
                builder.append(entry.getKey() + "=" + value);
            }
            if (iterator.hasNext()) {
                builder.append("&");
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Query : " + builder.toString());
        }
        return builder.toString();
    }


    private Response updateLCState(String assetType, String id, LCStateChange stateChange) throws RegistryException {
        String shortName = RESTUtil.getShortName(assetType);
        GenericArtifactManager manager = new GenericArtifactManager(getUserRegistry(), shortName);
        GenericArtifact artifact = manager.getGenericArtifact(id);
        if (artifact != null) {
            //TODO - change to Gov level
            getUserRegistry().invokeAspect(artifact.getPath(), stateChange.getLifecycle(),
                                           stateChange.getAction(), stateChange.getParameters());
            return getGovernanceAssetStates(artifact, null);
        }
        return Response.ok().build();
    }


    private Response getGovernanceAssetStates(String assetType, String id, String lcName) throws RegistryException {
        String shortName = RESTUtil.getShortName(assetType);
        GenericArtifactManager manager = new GenericArtifactManager(getUserRegistry(), shortName);
        GenericArtifact artifact = manager.getGenericArtifact(id);
        return getGovernanceAssetStates(artifact, lcName);
    }

    private Response getGovernanceAssetStates(GenericArtifact artifact, String lcName) throws RegistryException {
        AssetState assetState = null;
        if (artifact != null) {
            // lc == null means user look for all LCs
            if (lcName != null) {
                String state = artifact.getLifecycleState(lcName);
                assetState = new AssetState(state);
            } else {
                String[] stateNames = artifact.getLifecycleNames();
                if (stateNames != null) {
                    assetState = new AssetState();
                    for (String name : stateNames) {
                        assetState.addState(name, artifact.getLifecycleState(name));
                    }
                }
            }
        }
        return Response.ok().entity(assetState).build();
    }

    private Response modifyGovernanceAsset(String assetType, String id, DetachedGenericArtifact genericArtifact,
                                           String baseURL) throws RegistryException {
        String shortName = RESTUtil.getShortName(assetType);
        try {
            GenericArtifactManager manager = getGenericArtifactManager(shortName);
            GenericArtifact artifact = genericArtifact.makeRegistryAware(manager);
            artifact.setId(id);
            manager.updateGenericArtifact(artifact);
            URI link = new URL(RESTUtil.generateLink(assetType, id, baseURL)).toURI();
            return Response.created(link).build();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new GovernanceException(e);
        }
    }

    private Response persistGovernanceAsset(String assetType, DetachedGenericArtifact genericArtifact,
                                            String baseURL) throws RegistryException {
        String shortName = RESTUtil.getShortName(assetType);
        try {
            GenericArtifactManager manager = getGenericArtifactManager(shortName);
            GenericArtifact artifact = genericArtifact.makeRegistryAware(manager);
            manager.addGenericArtifact(artifact);
            URI link = new URL(RESTUtil.generateLink(assetType, artifact.getId(), baseURL, false)).toURI();
            return Response.created(link).build();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new GovernanceException(e);
        }
    }

    private Response deleteGovernanceAsset(String assetType, String id) throws RegistryException {
        String shortName = RESTUtil.getShortName(assetType);
        GenericArtifactManager manager = getGenericArtifactManager(shortName);
        GenericArtifact artifact = getUniqueAsset(shortName, id);
        if (artifact != null) {
            manager.removeGenericArtifact(artifact.getId());
            return Response.noContent().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }


    public Response getGovernanceAssets(String assetType, UriInfo uriInfo) throws RegistryException {
        String shortName = RESTUtil.getShortName(assetType);
        if (validateAssetType(shortName)) {
            PaginationInfo pagination = RESTUtil.getPaginationInfo(uriInfo.getQueryParameters());
            String query = createQuery(uriInfo);
            pagination.setQuery(query);
            List<GenericArtifact> artifacts = getAssetList(shortName, query, pagination);
            if (artifacts.size() > 0) {
                 if(artifacts.size() >= pagination.getCount()){
                     pagination.setMorePages(true);
                 }
                TypedList<GenericArtifact> typedList = new TypedList<>(GenericArtifact.class, shortName, artifacts, pagination);
                return Response.ok().entity(typedList).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } else {
            return validationFail(shortName);
        }
    }

    public Response getGovernanceAsset(String assetType, String id) throws RegistryException {
        String shortName = RESTUtil.getShortName(assetType);
        if (validateAssetType(shortName)) {
            GenericArtifact artifact = getUniqueAsset(shortName, id);
            if (artifact != null) {
                TypedList<GenericArtifact> typedList = new TypedList<>(GenericArtifact.class, shortName,
                                                                       Arrays.asList(artifact), null);
                return Response.ok().entity(typedList).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } else {
            return validationFail(shortName);
        }
    }

    private GenericArtifactManager getGenericArtifactManager(String shortName) throws RegistryException {
        return new GenericArtifactManager(getUserRegistry(), shortName);
    }

    private List<GenericArtifact> getAssetList(String assetType, String query, PaginationInfo pagination) throws RegistryException {
        Registry registry = getUserRegistry();
        GenericArtifactManager artifactManager = new GenericArtifactManager(registry, assetType);
        PaginationContext.init(pagination.getStart(), pagination.getCount(), pagination.getSortOrder(), pagination.getSortBy(),
                               pagination.getLimit());
        GenericArtifact[] genericArtifacts = artifactManager.findGovernanceArtifacts(query);
        PaginationContext.destroy();
        return Arrays.asList(genericArtifacts);
    }

    private GenericArtifact getUniqueAsset(String assetType, String id) throws RegistryException {
        Registry registry = getUserRegistry();
        GenericArtifactManager artifactManager = new GenericArtifactManager(registry, assetType);
        return artifactManager.getGenericArtifact(id);
    }

    private boolean validateAssetType(String assetType) throws RegistryException {
        if (assetType != null) {
            Registry registry = getUserRegistry();
            for (GovernanceArtifactConfiguration artifactConfiguration :
                    GovernanceUtils.findGovernanceArtifactConfigurations(registry)) {
                if (artifactConfiguration.getKey().equals(assetType)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected Registry getUserRegistry() throws RegistryException {
        CarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        RegistryService registryService = (RegistryService) carbonContext.
                getOSGiService(RegistryService.class, null);
        return registryService.getGovernanceUserRegistry(carbonContext.getUsername(), carbonContext.getTenantId());

    }

    private Response validationFail(String assetType) {
        return Response.status(Response.Status.NOT_FOUND).entity("Asset type " + assetType + " not found.").build();
    }

}

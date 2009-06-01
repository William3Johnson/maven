package org.apache.maven.project;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionErrorHandler;
import org.apache.maven.model.FileModelSource;
import org.apache.maven.model.ModelSource;
import org.apache.maven.model.Repository;
import org.apache.maven.model.resolver.InvalidRepositoryException;
import org.apache.maven.model.resolver.ModelResolver;
import org.apache.maven.model.resolver.UnresolvableModelException;
import org.apache.maven.repository.RepositorySystem;

/**
 * Implements a model resolver backed by the Maven Repository API.
 * 
 * @author Benjamin Bentmann
 */
class RepositoryModelResolver
    implements ModelResolver
{

    private RepositorySystem repositorySystem;

    private ResolutionErrorHandler resolutionErrorHandler;

    private ArtifactRepository localRepository;

    private List<ArtifactRepository> remoteRepositories;

    public RepositoryModelResolver( RepositorySystem repositorySystem, ResolutionErrorHandler resolutionErrorHandler,
                                    ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories )
    {
        if ( repositorySystem == null )
        {
            throw new IllegalArgumentException( "no repository system specified" );
        }
        this.repositorySystem = repositorySystem;

        if ( resolutionErrorHandler == null )
        {
            throw new IllegalArgumentException( "no resolution error handler specified" );
        }
        this.resolutionErrorHandler = resolutionErrorHandler;

        if ( localRepository == null )
        {
            throw new IllegalArgumentException( "no local repository specified" );
        }
        this.localRepository = localRepository;

        if ( remoteRepositories == null )
        {
            throw new IllegalArgumentException( "no remote repositories specified" );
        }
        this.remoteRepositories = new ArrayList<ArtifactRepository>( remoteRepositories );
    }

    public void addRepository( Repository repository )
        throws InvalidRepositoryException
    {
        try
        {
            ArtifactRepository repo = repositorySystem.buildArtifactRepository( repository );
            remoteRepositories.addAll( 0, repositorySystem.getMirrors( Arrays.asList( repo ) ) );
        }
        catch ( org.apache.maven.artifact.InvalidRepositoryException e )
        {
            throw new InvalidRepositoryException( "Failed to create artifact repository for " + repository.getId()
                + " with layout " + repository.getLayout() + " and URL " + repository.getUrl(), repository, e );
        }

    }

    public ModelSource resolveModel( String groupId, String artifactId, String version )
        throws UnresolvableModelException
    {
        Artifact artifactParent = repositorySystem.createProjectArtifact( groupId, artifactId, version );

        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact( artifactParent );
        request.setLocalRepository( localRepository );
        request.setRemoteRepostories( remoteRepositories );

        ArtifactResolutionResult result = repositorySystem.resolve( request );

        try
        {
            resolutionErrorHandler.throwErrors( request, result );
        }
        catch ( ArtifactResolutionException e )
        {
            throw new UnresolvableModelException( "Failed to resolve POM for " + groupId + ":" + artifactId + ":"
                + version, e );
        }

        return new FileModelSource( artifactParent.getFile() );
    }

}
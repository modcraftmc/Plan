/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan.delivery.webserver.pages;

import com.djrapitops.plan.delivery.web.resolver.NoAuthResolver;
import com.djrapitops.plan.delivery.web.resolver.Response;
import com.djrapitops.plan.delivery.web.resolver.request.Request;
import com.djrapitops.plan.delivery.webserver.response.ResponseFactory;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

/**
 * Resolves all static resources for the pages.
 *
 * @author Rsl1122
 */
@Singleton
public class StaticResourceResolver implements NoAuthResolver {

    private final ResponseFactory responseFactory;

    @Inject
    public StaticResourceResolver(ResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    @Override
    public Optional<Response> resolve(Request request) {
        return Optional.ofNullable(getResponse(request));
    }

    private Response getResponse(Request request) {
        String resource = request.getPath().asString();
        String filePath = "web" + resource;
        if (resource.endsWith(".css")) {
            return responseFactory.cssResponse(filePath);
        }
        if (resource.endsWith(".js")) {
            return responseFactory.javaScriptResponse(filePath);
        }
        if (resource.endsWith(".png")) {
            return responseFactory.imageResponse(filePath);
        }
        if (StringUtils.endsWithAny(resource, ".woff", ".woff2", ".eot", ".ttf")) {
            return responseFactory.fontResponse(filePath);
        }
        return null;
    }
}
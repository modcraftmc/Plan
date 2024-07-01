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
package net.playeranalytics.plan.modules.forge;

import com.djrapitops.plan.identification.properties.ServerProperties;
import dagger.Module;
import dagger.Provides;
import net.playeranalytics.plan.identification.properties.ForgeServerProperties;

import javax.inject.Singleton;

/**
 * Dagger module for providing ServerProperties on Fabric.
 *
 * @author Kopo942
 */
@Module
public class ForgeServerPropertiesModule {

    @Provides
    @Singleton
    ServerProperties provideServerProperties(ForgeServerProperties serverProperties) {
        return serverProperties;
    }
}

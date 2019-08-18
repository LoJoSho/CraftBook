/*
 * CraftBook Copyright (C) 2010-2018 sk89q <http://www.sk89q.com>
 * CraftBook Copyright (C) 2011-2018 me4502 <http://www.me4502.com>
 * CraftBook Copyright (C) Contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not,
 * see <http://www.gnu.org/licenses/>.
 */
package com.sk89q.craftbook.sponge.mechanics.boat;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import com.me4502.modularframework.module.Module;
import com.me4502.modularframework.module.guice.ModuleConfiguration;
import com.sk89q.craftbook.core.util.ConfigValue;
import com.sk89q.craftbook.core.util.CraftBookException;
import com.sk89q.craftbook.core.util.documentation.DocumentationProvider;
import com.sk89q.craftbook.sponge.CraftBookPlugin;
import com.sk89q.craftbook.sponge.mechanics.types.SpongeMechanic;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.manipulator.mutable.entity.PassengerData;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.RideEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;

@Module(id = "boatemptydecay", name = "BoatEmptyDecay", onEnable="onInitialize", onDisable="onDisable")
public class EmptyDecay extends SpongeMechanic implements DocumentationProvider {

    @Inject
    @ModuleConfiguration
    public ConfigurationNode config;

    private ConfigValue<Long> emptyTicks = new ConfigValue<>("empty-ticks", "The amount of time that the boat must be empty before it decays, in ticks.",
            40L, TypeToken.of(Long.class));
    private ConfigValue<Boolean> onlyOnExit = new ConfigValue<>("only-on-exit", "Only start the decay timer on exit, preventing boats being incorrectly removed.", true);

    @Override
    public void onInitialize() throws CraftBookException {
        emptyTicks.load(config);
        onlyOnExit.load(config);
    }

    @Listener
    public void onVehicleExit(RideEntityEvent.Dismount event) {
        if (event.getTargetEntity() instanceof Boat) {
            Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(emptyTicks.getValue()).execute(
                    new BoatDecay((Boat) event.getTargetEntity())).submit(CraftBookPlugin.inst());
        }
    }

    @Listener
    public void onEntityCreate(SpawnEntityEvent event) {
        if(onlyOnExit.getValue())
            return;
        event.getEntities().stream().filter(entity -> entity instanceof Boat).forEach(entity -> Sponge.getGame().getScheduler().createTaskBuilder()
                .delayTicks(emptyTicks.getValue()).execute(new BoatDecay((Boat) entity)).submit(CraftBookPlugin.inst()));
    }

    private static class BoatDecay implements Runnable {
        private Boat cart;

        BoatDecay(Boat cart) {
            this.cart = cart;
        }

        @Override
        public void run() {
            if (!cart.get(PassengerData.class).isPresent()) {
                cart.remove();
            }
        }
    }

    @Override
    public String getName() {
        return "Boat" + super.getName();
    }

    @Override
    public String getPath() {
        return "mechanics/boat/emptydecay";
    }

    @Override
    public ConfigValue<?>[] getConfigurationNodes() {
        return new ConfigValue<?>[] {
                emptyTicks,
                onlyOnExit
        };
    }
}

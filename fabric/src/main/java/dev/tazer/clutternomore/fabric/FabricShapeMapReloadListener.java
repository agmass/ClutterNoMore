//? if <1.21.9 {
/*package dev.tazer.clutternomore.fabric;

import dev.tazer.clutternomore.ClutterNoMore;
import dev.tazer.clutternomore.shape.ShapeMapReloadListener;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.Identifier;

public final class FabricShapeMapReloadListener extends ShapeMapReloadListener implements IdentifiableResourceReloadListener {

    @Override
    public Identifier getFabricId() {
        return ClutterNoMore.id(DIRECTORY);
    }
}
*///?}

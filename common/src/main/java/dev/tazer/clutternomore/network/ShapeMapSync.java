package dev.tazer.clutternomore.network;

import dev.tazer.clutternomore.ClutterNoMore;
import dev.tazer.clutternomore.mixin.creative.CreativeModeTabsAccessor;
import dev.tazer.clutternomore.recipe.RecipeFixes;
import dev.tazer.clutternomore.shape.ShapeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record ShapeMapSync(Map<Identifier, List<Identifier>> shapes, Map<Identifier, Identifier> parents, boolean recipeFixes) {

    public static ShapeMapSync read(FriendlyByteBuf buf) {
        int shapeCount = buf.readVarInt();
        Map<Identifier, List<Identifier>> shapes = new HashMap<>(shapeCount);
        for (int i = 0; i < shapeCount; i++) {
            Identifier parent = buf.readIdentifier();
            shapes.put(parent, readIds(buf));
        }
        int parentCount = buf.readVarInt();
        Map<Identifier, Identifier> parents = new HashMap<>(parentCount);
        for (int i = 0; i < parentCount; i++) {
            Identifier shape = buf.readIdentifier();
            parents.put(shape, buf.readIdentifier());
        }
        return new ShapeMapSync(shapes, parents, !buf.isReadable() || buf.readBoolean());
    }

    private static List<Identifier> readIds(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<Identifier> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(buf.readIdentifier());
        }
        return ids;
    }

    public static void write(FriendlyByteBuf buf, ShapeMapSync packet) {
        buf.writeVarInt(packet.shapes.size());
        for (Map.Entry<Identifier, List<Identifier>> entry : packet.shapes.entrySet()) {
            buf.writeIdentifier(entry.getKey());
            buf.writeVarInt(entry.getValue().size());
            for (Identifier id : entry.getValue()) {
                buf.writeIdentifier(id);
            }
        }
        buf.writeVarInt(packet.parents.size());
        for (Map.Entry<Identifier, Identifier> entry : packet.parents.entrySet()) {
            buf.writeIdentifier(entry.getKey());
            buf.writeIdentifier(entry.getValue());
        }
        buf.writeBoolean(packet.recipeFixes);
    }

    public static ShapeMapSync capture() {
        Map<Identifier, List<Identifier>> shapes = new HashMap<>();
        for (Map.Entry<Item, List<Item>> entry : ShapeMap.getShapesByParent().entrySet()) {
            List<Identifier> ids = new ArrayList<>(entry.getValue().size());
            for (Item shape : entry.getValue()) {
                ids.add(BuiltInRegistries.ITEM.getKey(shape));
            }
            shapes.put(BuiltInRegistries.ITEM.getKey(entry.getKey()), ids);
        }
        Map<Identifier, Identifier> parents = new HashMap<>();
        for (Map.Entry<Item, Item> entry : ShapeMap.getParentByShape().entrySet()) {
            parents.put(BuiltInRegistries.ITEM.getKey(entry.getKey()), BuiltInRegistries.ITEM.getKey(entry.getValue()));
        }
        return new ShapeMapSync(shapes, parents, ClutterNoMore.STARTUP_CONFIG.recipeFixes.value());
    }

    public static void apply(ShapeMapSync packet, boolean fromRemoteServer) {
        Map<Item, List<Item>> shapesByParent = new HashMap<>();
        for (Map.Entry<Identifier, List<Identifier>> entry : packet.shapes.entrySet()) {
            Optional<Item> parent = BuiltInRegistries.ITEM.getOptional(entry.getKey());
            if (parent.isPresent()) {
                List<Item> shapes = new ArrayList<>(entry.getValue().size());
                for (Identifier id : entry.getValue()) {
                    BuiltInRegistries.ITEM.getOptional(id).ifPresent(shapes::add);
                }
                shapesByParent.put(parent.get(), shapes);
            }
        }
        Map<Item, Item> parentByShape = new HashMap<>();
        for (Map.Entry<Identifier, Identifier> entry : packet.parents.entrySet()) {
            Optional<Item> shape = BuiltInRegistries.ITEM.getOptional(entry.getKey());
            Optional<Item> parent = BuiltInRegistries.ITEM.getOptional(entry.getValue());
            if (shape.isPresent() && parent.isPresent()) {
                parentByShape.put(shape.get(), parent.get());
            }
        }
        ShapeMap.setShapeMaps(shapesByParent, parentByShape, fromRemoteServer);
        RecipeFixes.setEnabled(packet.recipeFixes);
        CreativeModeTabsAccessor.cnm$setCachedParameters(null);
        ClutterNoMore.LOGGER.info("[ShapeMap] Received {} shape sets from the server", shapesByParent.size());
    }
}

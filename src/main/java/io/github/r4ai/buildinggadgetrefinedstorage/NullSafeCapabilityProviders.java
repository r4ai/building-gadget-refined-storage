package io.github.r4ai.buildinggadgetrefinedstorage;

import com.refinedmods.refinedstorage.common.api.support.network.NetworkNodeContainerProvider;
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.BridgeBackend;
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.FluidProxyFluidHandler;
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.FluidProxyRef;
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.platform.BridgeLookup;
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.platform.RefinedStorageBridgeBlockEntity;
import io.github.r4ai.buildinggadgetrefinedstorage.content.ModContent;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

final class NullSafeCapabilityProviders {
    private NullSafeCapabilityProviders() {
    }

    static ICapabilityProvider<RefinedStorageBridgeBlockEntity, @Nullable Direction, IItemHandler> itemHandlerProvider() {
        return (blockEntity, context) -> blockEntity.getItemHandler();
    }

    static ICapabilityProvider<RefinedStorageBridgeBlockEntity, @Nullable Direction, NetworkNodeContainerProvider> networkNodeContainerProvider() {
        return (blockEntity, context) -> blockEntity.getContainerProvider();
    }

    static ICapabilityProvider<ItemStack, @Nullable Void, IFluidHandlerItem> fluidProxyItemProvider() {
        return (stack, context) -> {
            FluidProxyRef proxyRef = ModContent.INSTANCE.fluidProxyRef(stack);
            if (proxyRef == null) {
                return null;
            }
            return new FluidProxyFluidHandler(stack, proxyRef, (FluidProxyRef argument) -> BridgeLookup.INSTANCE.resolveBackend(argument));
        };
    }
}

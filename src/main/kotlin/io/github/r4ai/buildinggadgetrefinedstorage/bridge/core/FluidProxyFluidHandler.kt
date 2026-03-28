package io.github.r4ai.buildinggadgetrefinedstorage.bridge.core

import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.FluidType
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem

class FluidProxyFluidHandler(
    private val container: ItemStack,
    private val proxyRef: FluidProxyRef,
    private val backendResolver: (FluidProxyRef) -> BridgeBackend?,
) : IFluidHandlerItem {
    override fun getContainer(): ItemStack = container

    override fun getTanks(): Int = 1

    override fun getFluidInTank(tank: Int): FluidStack {
        if (tank != 0) {
            return FluidStack.EMPTY
        }
        return when (proxyRef.mode) {
            FluidProxyMode.SPECIFIC -> proxyRef.descriptor?.toFluidStack(FluidType.BUCKET_VOLUME) ?: FluidStack.EMPTY
            FluidProxyMode.WILDCARD_INPUT -> FluidStack.EMPTY
        }
    }

    override fun getTankCapacity(tank: Int): Int = if (tank == 0) Int.MAX_VALUE else 0

    override fun isFluidValid(tank: Int, stack: FluidStack): Boolean {
        if (tank != 0 || stack.isEmpty) {
            return false
        }
        return when (proxyRef.mode) {
            FluidProxyMode.SPECIFIC -> proxyRef.descriptor?.matches(stack) == true
            FluidProxyMode.WILDCARD_INPUT -> true
        }
    }

    override fun fill(resource: FluidStack, action: IFluidHandler.FluidAction): Int {
        if (!isFluidValid(0, resource)) {
            return 0
        }
        val backend = backendResolver(proxyRef)?.takeIf(BridgeBackend::isActive) ?: return 0
        return backend.insertFluid(resource, action == IFluidHandler.FluidAction.SIMULATE)
    }

    override fun drain(resource: FluidStack, action: IFluidHandler.FluidAction): FluidStack {
        if (resource.isEmpty || proxyRef.mode != FluidProxyMode.SPECIFIC) {
            return FluidStack.EMPTY
        }
        val descriptor = proxyRef.descriptor ?: return FluidStack.EMPTY
        if (!descriptor.matches(resource)) {
            return FluidStack.EMPTY
        }
        return drain(resource.amount, action)
    }

    override fun drain(maxDrain: Int, action: IFluidHandler.FluidAction): FluidStack {
        if (maxDrain <= 0 || proxyRef.mode != FluidProxyMode.SPECIFIC) {
            return FluidStack.EMPTY
        }
        val descriptor = proxyRef.descriptor ?: return FluidStack.EMPTY
        val backend = backendResolver(proxyRef)?.takeIf(BridgeBackend::isActive) ?: return FluidStack.EMPTY
        return backend.extractFluid(descriptor, maxDrain, action == IFluidHandler.FluidAction.SIMULATE)
    }
}


package io.github.r4ai.buildinggadgetrefinedstorage.bridge.platform

import com.refinedmods.refinedstorage.api.network.Network
import com.refinedmods.refinedstorage.api.network.node.NetworkNode

class RefinedStorageBridgeNetworkNode : NetworkNode {
    private var network: Network? = null
    var active: Boolean = false
        private set

    override fun getNetwork(): Network? = network

    override fun setNetwork(network: Network?) {
        this.network = network
    }

    fun setActive(active: Boolean) {
        this.active = active
    }
}


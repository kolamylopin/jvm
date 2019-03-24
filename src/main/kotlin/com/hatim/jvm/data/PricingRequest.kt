package com.hatim.jvm.data

import net.openhft.chronicle.wire.Marshallable

data class PricingRequest(val id: String? = null,
                          val destination: String? = null,
                          val message: String? = null,
                          val timestamp: Long = 0) : Marshallable

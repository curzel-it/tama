package it.curzel.tama

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
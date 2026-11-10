package wgzu.github.io.workshop

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
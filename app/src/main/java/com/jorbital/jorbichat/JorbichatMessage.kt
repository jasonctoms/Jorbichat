package com.jorbital.jorbichat

class JorbichatMessage(var text: String? = null, name: String = "", photoUrl: String = "", var imageUrl: String? = null) {
    var id: String? = null
    var name: String? = name
    var photoUrl: String? = photoUrl
}
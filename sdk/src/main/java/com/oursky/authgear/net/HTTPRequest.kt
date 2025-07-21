package com.oursky.authgear.net

import java.io.InputStream
import java.net.URI

public class HTTPRequest internal constructor(
    public val method: String,
    public val headers: MutableMap<String, MutableList<String>>,
    public val uri: URI,
    public val body: InputStream? = null,
    public val followRedirect: Boolean? = null
)

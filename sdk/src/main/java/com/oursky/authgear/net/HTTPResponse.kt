package com.oursky.authgear.net

import java.io.InputStream

public class HTTPResponse internal constructor(
    public val statusCode: Int,
    public val headers: Map<String, List<String>>,
    internal val body: InputStream
)

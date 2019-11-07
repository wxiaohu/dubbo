package com.alibaba.dubbo.common.extensionloader.adaptive;

import com.alibaba.dubbo.common.extension.ExtensionLoader;

public class HasAdaptiveExt$Adaptive implements com.alibaba.dubbo.common.extensionloader.adaptive.HasAdaptiveExt {
    public java.lang.String echo(com.alibaba.dubbo.common.URL arg0, java.lang.String arg1) {
        if (arg0 == null) throw new IllegalArgumentException("url == null");
        com.alibaba.dubbo.common.URL url = arg0;
        String extName = url.getParameter("has.adaptive.ext");
        if (extName == null)
            throw new IllegalStateException("Fail to get extension(com.alibaba.dubbo.common.extensionloader.adaptive.HasAdaptiveExt) name from url(" + url.toString() + ") use keys([has.adaptive.ext])");
        com.alibaba.dubbo.common.extensionloader.adaptive.HasAdaptiveExt extension = (com.alibaba.dubbo.common.extensionloader.adaptive.HasAdaptiveExt) ExtensionLoader.getExtensionLoader(com.alibaba.dubbo.common.extensionloader.adaptive.HasAdaptiveExt.class).getExtension(extName);
        return extension.echo(arg0, arg1);
    }
}
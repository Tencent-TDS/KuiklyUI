package com.tencent.kuikly.core.render.web.expand.components

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.Frame
import com.tencent.kuikly.core.render.web.const.KRCssConst
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.ktx.setFrame
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import org.w3c.dom.HTMLElement

/**
 * Loading spinner view
 */
class KRActivityIndicatorView : IKuiklyRenderViewExport {
    private val acIndicator = kuiklyDocument.createElement(ElementType.DIV)

    // Whether it's in gray style
    private var isGrayMode = false

    override val ele: HTMLElement
        get() = acIndicator.unsafeCast<HTMLElement>()

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            PROPS_STYLE -> {
                isGrayMode = propValue.unsafeCast<String>() == GRAY_STYLE
                true
            }

            KRCssConst.FRAME -> {
                // Set acIndicator size, need to set style simultaneously
                val frame = propValue.unsafeCast<Frame>()
                // First set ac size
                ele.setFrame(frame, ele.style)
                // Then set loading indicator style
                ele.style.backgroundImage =
                    "url(data:image/png;base64,${if (isGrayMode) GRAY_IMAGE else WHITE_IMAGE})"
                // Background image scaling
                ele.style.backgroundSize = "contain"
                // Set animation (840ms rotation speed, same as iOS classic loading indicator)
                ele.style.animation = "activityIndicatorRotate 840ms linear infinite"
                // If setting size, notify that size information has changed
                onFrameChange(frame)
                true
            }

            else -> super.setProp(propKey, propValue)
        }
    }

    companion object {
        const val VIEW_NAME = "KRActivityIndicatorView"
        const val PROPS_STYLE = "style"
        const val GRAY_STYLE = "gray"
        const val WHITE_STYLE = "white"
        const val GRAY_IMAGE =
            "iVBORw0KGgoAAAANSUhEUgAAAJYAAACWCAMAAAAL34HQAAAAS1BMVEUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADmYDp0AAAAGXRSTlMAJk0zQGZzBCITGh8OCggXSTotRDEqXVVqQNv4pgAABP9JREFUeNrs2ctyozAQheHTGKEbEtgk2O//pJOaSSjUQUiAqGHhb+vNX6ZpjIy3t6vTlshqXItT9JdyuBBHkyt1KZooXIammevMl6UZi6ugAK7iP2ZJ83iYI1m+bT1Ku72aL6/b3iyt6IvSKGpovlX7sgx961BQ30zqPVkdTQyKsc2M2p7laMahlKqZEXJzlqIZg1JEM3fbmqVpTqGUJvBy27IkhVDKqwn027I6CqiCsxXqtmR5ChmUQizruSXLUqhFMSPr+szPailkUY5hWUJmZykKOTAlp+uem6WLThbXNkyblyWJkSjqzrKqvCxDIY2ypGBdJifLUUihtE+WNUazzl0O3JN1PWJZ5y4HrmNZL5/KkopCHifoWVedytIU6nAG92JdOpJ18nLgbnxJxF5f08vh7CURf9n3RZeDGUYx9gpLFMvq145GTNbDsDWKlHFIqMU/T5OxJMTaQZLKeBg6yz6NVk0GB3C6Ca0du6WXgzSZd+lDzN0kuCFcXWuHlCq1HHTuDSFHERgVGB8siSdi+GwpCaZV2XdEJ7hKI/TBfqTGtWvfhbfEecSQ+K120akfscpG51129FubyGLGDxle6OkSeqySU5eNDFVmVicWPS1mXPW9tKba1HgZyc7KF3lEjWJZrzHT3obhQyOD1F2ngyhvaJnKWhDc3aMA2VGMxopBxIwPHKYVxRisuouoXuIYQ1EdEuxTxDw9DpCWYpRDkvwYRcRwznelkcXVIqLFbpIiOolcuhKLHthN0yLrsYUaF9cEdutogWqxkbydn6WxgxvOvYhGYh/zPG/krcN+FIxYjQMMG6pD5F1MKlnqz1AtcVTb/1R5HCItG6qDupq9Om7DXwo93t7e3gqQVPfVor4miZK0pQirEdJDtWLQKMYpWqFcUFUl6GJVlDDrkkOVMEiUoShBYUJVEqEITUkaP+oqqUYRlpIsfvRVUo8iKMPVs/60c7Y7DoJAFKVAguInFN7/VTfpbqnedTRt6jA/PE9wVLzKMCD0IQod8kIDQmicSv34SP1UX1xcXHxGm+7zHLys0shg9C/BySkkuawLQUzZrZv1giSjSNlavWb4Ykl3/HxQIU31Ari7zfofsfZygb/rDWLdxZU+6E2amktRY9IEQ8WFu2bWBKbeMud01xS23qJw1iSh3hK60RRzU6/hoNEU0VVsz6AGe+ixIcUPHzazdO83s7TE7HVa/1PYB5mt9afbHlRuZWXsH8kxNEpRWgmuItqCYWora8lBVfB2wcTThIdD/u4V4IJdEDlaFjEg5o2SyM0uCQtfn1PK3n2nwROJumCGre6bNa9rj+ZB7L/TDosk/YvdtE92+271ptATdb/uveZhpDX0pLC1a/LzsqMpRHdCq/U+AbT6p4FZ4MnG9JPoig+8iNksyMxt/CPerHLrk1mQmDc93MDq9VTMCt4tIj1YBUdo8W6oSaDlFaHFuv1oAiujKC3OzVqOCAdCiyskGiJJCS2mkBgtMO5rMYVEJsKB0GLYZEqEA63FtyXXgNakDrRYQqIFq6SOtFhCImI47GqxbY6HzLqpXS22owTo8U5r0SFx0t3q1L4W2zEVGX7g97V4DvXAN7F9X2s4Kbgy5PuuFseBMfhnmtWh1pnH6yA+PMZVp461zj2MCOm9hxglteijmzhBLSnAhEwKMH2VAkz2pQClETGsCkmCKGU3UValSCnoCV5cbPIDQR5TIrqY83gAAAAASUVORK5CYII="
        const val WHITE_IMAGE =
            "iVBORw0KGgoAAAANSUhEUgAAAJYAAACWCAMAAAAL34HQAAAAb1BMVEUAAAD///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////8v0wLRAAAAJHRSTlMATWZZv5mM2UUHIDM+FBo4JgwrEVJ9qDCHbbd0XejRx5D2sZ/haBjGAAAFg0lEQVR42uzZW5eaMBSG4W9TMEfOHaxOFUfz/39ju9oZS5AQDnGVC59Lr94VN1tAvLxsXaGJdIFtSTn9wVNsSEp3W+ridMexGQV1bGe+NHVobAVZsBX/MUvkUaTXZKVS1gjtcDK/nd6XZklOv3GJoBLzqVmWpejTHgE15m63JKuiO4VguOmg+VkZdaQBD6uDidlZnOgZx3UyXe9zsyR1cYTyYWV9ZPOyBNkCnpblNi9rT086rauxqTlZNdlyhPLd2No5WZpsGYI5G9u36VkZ2TTC0cZ2EpOzONnSsGve9jY1qyCbQkjyw1g+5LQsQT0CQb0ZWzMtS5GtQFjiZGx6SlZKNo7Qvhnb2ZPlWA7BtcYW+bMk2TTCU/0lUfqyBCdbjSe4GdvOl1WRbY9nyPpLohjPKqlH4Cneje06/viak03iOQQztnzsYb8OuhzyS3xuE44h1N+pY69G1KTlkOWcuErhsWN/XRUGHHsX49iLJD7hNivV034qf7C7S4YHVW/ox167ka3EA6EmXqU/Wcf5INCX9E/L/ZKSU1eFB8XUC0K0zBITeuqT6ThiRE4dXKAn45OvCMX6mgq2Q+8m1S0bO4taU18Nl+/s0Y/UOfUtRmnnvIs9PZKerJ5zJNBRnu9VKUYJ7fiJlkSzshQbFHN0pI354ybg83koSjiGypLCqWXDbgU65CFJDhITlLKqZGl9ktMwDreIubzVCEBU5CIx4sJc2p9YTXJyURj1xpySEuvk5FTBg8fM5VpjBaHJhWfwEtGZOVywgvKMlVf6gzlkWKwkh73AVFXDBq0Ye0mDdI05KB5cE1isWjZUfeJwfnpWgQWyy3O/RCWwjLoymww38jrFct9a1rHDCnvq4BKrlO//RqwRof4MrQTWkslXVQ0EWfN5iRDULj63N8JC/YfCGi8vLy8BiCg5xoOOSSQQUqHJQRewqSYe0SgEk3IawVOrKvZQwarIo9MlmtijEQiDkwfHXRR7RQiiIK8CX5LYK0EQmrw0vhxjryOCoAm2nvWrXbtZchSEogB85RYUoPEnwVJLF/3+TzlVdoeBO9iMUUoWfttkccqQo8LN9EfMdMlnWhCZ1mmuN59cb9W32+32Gc1GLqYmr62ReuLf+i6fjSRluDVns+1WCe5geWxS6p77Hhls6b4Yp+TlG+Cq4P/Cq48LmpEH4LWHK3rmQfLKo6hu4RseFx7cyS++gV13zDmMfEvfXXYojHzTpC47Qmd8y5e8buBA8i3YXTieIXjYXPujc8b8372ja8uyPTzMonnQOPj1IVao4osJV83B0Z8mvKgUONQsfkyR31VJ/CHVzkGpeCxGapgJa4r0H1rVobEyzamZfrkSjhJ+odHRHhnCo0t+bIDoRuFgsYtlmQMji7QgCgWUEa7enSE1jBn3Ecqgq9w14EktZFERtfCMfz9AtsIa3tD3+mgcltZ83wY/FeGrVTPL5irQIyPDwxHt+lIY/tpT+BC+KWQWKltavsio9QE9ifVeDyVzlO8rgb4CEpEkFbOrmzmM7RL0PSGJbiQL3l565rHfR0JBCih8BfweCwb0NZBATVL1KhZLGfQ94HwTidVALBZopCVxupKkmiEeCyT6NJxMBcohHouWhAErTTksEIkV7tQBTvWi5fCKxNoqiQ7OtAifhHis1TNlSeiNcojHgiJhScwk1gDxWOlLYiCpJojG2i6JGs7CSKx6T6wX+io4C+kshD2xoEnVXaNfDt2+WAp9ia6WhF2xaEkUaVqrh72xoEjTXK1wPPfHqtP8EwHJw9/OWFAmqnmba4FPYkGV6Am16td1VcFnseBp1nJo4Wy6aTTAjljEQ+sHpBOPlYtMY5HX11yQl/1ckK2RbHgbSRmx225ZpbKblBn9grdb0B9tgZ0KTsLMSAAAAABJRU5ErkJggg=="
    }
}

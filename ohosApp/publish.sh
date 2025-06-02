pwd
which ohpm
ohpm -v
which hvigorw
ohpm install --all --strict_ssl true
hvigorw clean --no-daemon
hvigorw assembleHar --mode module -p module=render@default -p product=default -p buildMode=release --no-daemon
#ohpm dist-tags remove @kuikly-open/render alpha
ohpm publish ../core-render-ohos/build/default/outputs/default/render.har

#!/bin/sh

set -e

echo "working path: $(pwd)"
pushd ohosApp
SDK_HOME=/Applications/DevEco-Studio.app/Contents
export DEVECO_SDK_HOME=$SDK_HOME/sdk
export PATH=$DEVECO_SDK_HOME:$SDK_HOME/jbr/Contents/Home/bin:$SDK_HOME/tools/node/bin:$SDK_HOME/tools/ohpm/bin:$SDK_HOME/tools/hvigor/bin:$PATH

$SDK_HOME/tools/ohpm/bin/ohpm install --all
$SDK_HOME/tools/node/bin/node $SDK_HOME/tools/hvigor/bin/hvigorw.js --sync -p product=default --analyze=normal --parallel
$SDK_HOME/tools/node/bin/node $SDK_HOME/tools/hvigor/bin/hvigorw.js --mode module -p module=entry@default -p product=default -p requiredDeviceType=phone assembleHap --analyze=normal --parallel

HDC_BIN=$SDK_HOME/sdk/default/openharmony/toolchains/hdc
targets=$($HDC_BIN list targets)
HAP_PATH=entry/build/default/outputs/default
if [ -z "$targets" ]; then
  echo "error: 先启动模拟器"
  exit 1
elif [ -e "$HAP_PATH/entry-default-unsigned.hap" ] && [ ! -e "$HAP_PATH/entry-default-signed.hap" ]; then
  echo "error: 先配置签名 参考: https://developer.huawei.com/consumer/cn/doc/HMSCore-Guides/harmonyos-java-config-app-signing-0000001199536987"
  exit 2
else
  for target_id in $($HDC_BIN list targets); do
  echo "install to $target_id"
  $HDC_BIN -t "$target_id" shell aa force-stop com.example.kuiklyworkwithminiapp
  $HDC_BIN -t "$target_id" install entry/build/default/outputs/default/entry-default-signed.hap
  $HDC_BIN -t "$target_id" shell aa start -a EntryAbility -b com.example.kuiklyworkwithminiapp
  done
fi
import { ImageCallback } from '@kuikly-open/render';
import { http } from '@kit.NetworkKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { image } from '@kit.ImageKit';

function isBase64(src: string): boolean {
  return src.startsWith('data:image');
}

function isFile(src: string): boolean {
  return src.startsWith('file://');
}

function isAssets(src: string): boolean {
  return src.startsWith('assets://');
}

function isNetwork(src: string): boolean {
  return src.startsWith('http://') || src.startsWith('https://');
}

function downloadFile(imageSrc: string, callback: ImageCallback) {
  let OutData: http.HttpResponse;
  http.createHttp().request(imageSrc,
    (error: BusinessError, data: http.HttpResponse) => {
      if (error) {
        console.error(`http request failed with. Code: ${error.code}, message: ${error.message}`);
      } else {
        OutData = data;
        let code: http.ResponseCode | number = OutData.responseCode;
        if (http.ResponseCode.OK === code) {
          let imageData: ArrayBuffer = OutData.result as ArrayBuffer;
          let imageSource: image.ImageSource = image.createImageSource(imageData);
          // 配置解码选项参数。
          let decodingOptions : image.DecodingOptions = {
            editable: false,
            desiredPixelFormat: image.PixelMapFormat.RGBA_8888,
          };
          // 创建pixelMap。
          imageSource.createPixelMap(decodingOptions).then((pixelMap : image.PixelMap) => {
            callback("", pixelMap);
          }).catch((err : BusinessError) => {
            callback("", null);
          });
        }
      }
    }
  )
}

function getPixelMapFromFile(filePath: string, callback: ImageCallback) {
  const imageSource : image.ImageSource = image.createImageSource(filePath);
  if (!imageSource) {
    callback(filePath, null);
    return;
  }
  // 配置解码选项参数。
  let decodingOptions : image.DecodingOptions = {
    editable: false,
    desiredPixelFormat: image.PixelMapFormat.RGBA_8888,
  };
  // 创建pixelMap。
  let result = imageSource.createPixelMapSync(decodingOptions);
  callback("", result);
}


export function AppKRImageAdapter(imageSrc: string, callback: ImageCallback) {
  if (isBase64(imageSrc)) {
    // 直接用imageSrc作为图片资源，kuikly内部加载
    callback(imageSrc, null);
  } else if (isFile(imageSrc)) {
    callback(imageSrc, null);
    getPixelMapFromFile(imageSrc, callback);
  } if (isAssets(imageSrc)) {
    // assets://xxx.png -> resFileDir/xxx.png
    const kAssetsPrefix = 'assets://';
    const imageSrcWithoutPrefix = imageSrc.substring(kAssetsPrefix.length);
    let filePath = getContext().resourceDir + '/' + imageSrcWithoutPrefix;
    getPixelMapFromFile(filePath, callback);
  } else if (isNetwork(imageSrc)) {
    // 网络图片，异步下载
    downloadFile(imageSrc, callback);
  }
}

/***
*    !!!  GEN CODE DO NOT EDIT  !!!
***/
import { getService } from "@kuiklybase/knoi"

export interface KuiklyDemoBuglyService {
            
   registerUnhandledExceptionHook(reportOperation: Function): void;

}

export function getKuiklyDemoBuglyService(): KuiklyDemoBuglyService {
   return getService<KuiklyDemoBuglyService>("KuiklyDemoBuglyService")
}


# Order


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **string** |  | [optional] [default to undefined]
**clientId** | **string** |  | [optional] [default to undefined]
**date** | **string** |  | [optional] [default to undefined]
**location** | [**Point**](Point.md) |  | [optional] [default to undefined]
**requestedGLP** | **number** |  | [optional] [default to undefined]
**deliveredGLP** | **number** |  | [optional] [default to undefined]
**deliveryLimit** | **string** |  | [optional] [default to undefined]
**minDeliveryDate** | **string** |  | [optional] [default to undefined]
**maxDeliveryDate** | **string** |  | [optional] [default to undefined]

## Example

```typescript
import { Order } from './api';

const instance: Order = {
    id,
    clientId,
    date,
    location,
    requestedGLP,
    deliveredGLP,
    deliveryLimit,
    minDeliveryDate,
    maxDeliveryDate,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)

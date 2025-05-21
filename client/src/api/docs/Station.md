# Station


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **string** |  | [optional] [default to undefined]
**name** | **string** |  | [optional] [default to undefined]
**location** | [**Point**](Point.md) |  | [optional] [default to undefined]
**maxCapacity** | **number** |  | [optional] [default to undefined]
**mainStation** | **boolean** |  | [optional] [default to undefined]
**availableCapacityPerDate** | **{ [key: string]: number; }** |  | [optional] [default to undefined]

## Example

```typescript
import { Station } from './api';

const instance: Station = {
    id,
    name,
    location,
    maxCapacity,
    mainStation,
    availableCapacityPerDate,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)

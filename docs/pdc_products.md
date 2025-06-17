# PDC Products Handling

The API receives `PRODUCT` messages from the PDC SQS queue. Each message is stored
as JSON in an AWS S3 bucket configured by `pdcProduct.s3Bucket` and
`pdcProduct.s3Folder`. The filename is based on the product UUID contained in the
SQS payload.

If a file with the same UUID already exists in the bucket it is not overwritten.
This prevents duplicates when the same product arrives from both SQS and the
HpSrv service.


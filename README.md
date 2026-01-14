# microservice-workspace

# Generación del código base y gRPC para role.proto
protoc -I=protobuf --go_out=genproto/go/example --go_opt=paths=source_relative protobuf/example.proto
protoc -I=protobuf --go-grpc_out=genproto/go/example --go-grpc_opt=paths=source_relative protobuf/example.proto


# Go
protoc -I=protobuf --go_out=genproto/go/identity --go_opt=paths=source_relative protobuf/identity.proto
protoc -I=protobuf --go-grpc_out=genproto/go/identity --go-grpc_opt=paths=source_relative protobuf/identity.proto

# Accounts
protoc -I=proto --go_out=proto/genproto/go/accounts --go_opt=paths=source_relative proto/accounts.proto
protoc -I=proto --go-grpc_out=proto/genproto/go/accounts --go-grpc_opt=paths=source_relative proto/accounts.proto


protoc --go_out=genproto/go/accounts --go_opt=paths=source_relative proto/accounts.proto
protoc --go-grpc_out=genproto/go/accounts --go-grpc_opt=paths=source_relative proto/accounts.proto


protoc -I=proto --go_out=proto/genproto/go/sof --go_opt=paths=source_relative proto/sof_records.proto
protoc -I=proto --go-grpc_out=proto/genproto/go/sof --go-grpc_opt=paths=source_relative proto/sof_records.proto

# Testing
### Install testing dependencies
```
go get github.com/stretchr/testify@latest
go get github.com/testcontainers/testcontainers-go@latest
```

### Remove file cover if command cover exist on Windows
```
Remove-Item .\cover -ErrorAction SilentlyContinue
```
### Then execute test command & generate cover.out
```
go test ./test/unit/... -coverpkg ./internal/... -coverprofile cover.out -count 1
```
### Generate coverage html file
```
go tool cover -func cover.out
go tool cover -html cover.out -o coverage.html
```
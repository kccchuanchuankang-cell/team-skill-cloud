# ErrorCategory
1. BUSINESS（DS内部业务报错：如参数错误、数据错误等）(可预见的)
2. SYSTEM （DS内部系统报错： 如内部组件调用网络超时、NPE、数组越界等）（不可预见，或理论上不应该发生的）
3. THIRD_PARTY （DS外部依赖报错： 调用外部系统超时、404、502都应归为此类）
4. SUPPLIER （DS依赖供应商系统报错）



# ErrorCode

1. 命名规范
   1. 首字母大写，驼峰格式，名词+过去分词
   如SupplierNotFound，SupplierHotelNotFound，RequestLimitExceeded




# ErrorMsg

1. ErrorCategory.BUSINESS

   1. 全英文，首字母大写，需要具备可读性，尽量简短，可加入具体标识便于问题定位（如酒店id，订单号，请放入[]中），不可包含敏感信息或组件信息（如表名，URL等） 
   如 Supplier Hotel [123456] not found

2. ErrorCategory.SYSTEM
   1. 若是可以预见的报错，自行编写msg内容，则要求：全英文，首字母大写，需要具备可读性，尽量简短，可加入具体标识便于问题定位（如酒店id，订单号，请放入[]中），不可包含敏感信息或组件信息（如表名，URL等） 
   2. 不可预见的报错：e.getMessage

3. ErrorCategory.THIRD_PARTY
   1. 第三方服务报错信息即可

4. ErrorCategory.SUPPLIER
   1. 供应商服务报错信息即可




# ErrorSource

1. ErrorCategory.BUSINESS / ErrorCategory.SYSTEM
   1. 对于starry组件，使用appName
   2. 对于DS其他团队组件，全部小写，已'-'分隔，如drs、dswitch-router等，对共同使用的组件名，需维护在公共包中

2. ErrorCategory.THIRD_PARTY / ErrorCategory.SUPPLIER
   1. 优先使用SupplierCode和DistributorCode
   2. 非SupplierCode和DistributorCode，统一定义，全部小写，已'-'分隔，如google
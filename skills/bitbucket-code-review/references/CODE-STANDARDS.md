# 命名规范
1. 【强制】代码中的命名均不能以下划线或美元符号开始，也不能以下划线或美元符号结束。
反例： _name / _name / $Object / name / name$ / Object$
2. 【强制】代码中的命名严禁使用拼音与英文混合的方式，更不允许直接使用中文的方式。说明：正确的英文拼写和语法可以让阅读者易于理解，避免歧义。注意，即使纯拼音命名方式也要避免采用。
正例： alibaba / taobao / youku / hangzhou 等国际通用的名称，可视同英文。
反例： DaZhePromotion [打折] / getPingfenByName() [评分] / int 某变量 = 3
3. 【强制】类名使用 UpperCamelCase 风格，必须遵从驼峰形式，但以下情形例外：DO / BO / DTO / VO / AO
正例： MarcoPolo / UserDO / XmlService / TcpUdpDeal / TaPromotion
反例： macroPolo / UserDo / XMLService / TCPUDPDeal / TAPromotion
4. 【强制】方法名、参数名、成员变量、局部变量都统一使用 lowerCamelCase 风格，必须遵从驼峰形式。
正例： localValue / getHttpMessage() / inputUserId
5. 【强制】常量命名全部大写，单词间用下划线隔开，力求语义表达完整清楚，不要嫌名字长。
正例： MAX_STOCK_COUNT
反例： MAX_COUNT

# 常量定义
1. 【强制】不允许任何魔法值（即未经定义的常量）直接出现在代码中。

# OOP 规约
1. 【强制】避免通过一个类的对象引用访问此类的静态变量或静态方法，无谓增加编译器解析成本，直接用类名来访问即可。
2. 【强制】所有的覆写方法，必须加@Override 注解。
3. 【强制】Object 的 equals 方法容易抛空指针异常，应使用常量或确定有值的对象来调用 equals。
正例： "test".equals(object);
反例： object.equals("test");
4. 【强制】所有的相同类型的包装类对象之间值的比较，全部使用 equals 方法比较。
说明：对于 Integer var = ? 在-128 至 127 范围内的赋值，Integer 对象是在 IntegerCache.cache 产生，会复用已有对象，这个区间内的 Integer 值可以直接使用 == 进行判断，但是这个区间之外的所有数据，都会在堆上产生，并不会复用已有对象，这是一个大坑，推荐使用 equals 方法进行判断。


# 集合处理
1. 【强制】关于 hashCode 和 equals 的处理，只要重写 equals，就必须重写 hashCode
2. 【强制】禁止使用Arrays.asList(), List.of(), Map.of()方法
3. 【强制】Comparator 要满足如下三个条件
   1. x，y 的比较结果和 y，x 的比较结果相反。
   2. x>y，y>z，则 x>z。
   3. x=y，则 x，z 比较结果和 y，z 比较结果相同。


# 日志规约
1. PerfDuration名称需要与方法名相同
2. TraceDuration名称需要与方法名相同


# 开关配置
1. Properties配置开关的命名规则是： xxx.yyy.zzz.enabled=true，
2. 代码里为enableXxxYyyZzz=true

# BigDecimal使用规范
1. 【强制】等值比较时，必须使用 compareTo 方法，禁止使用 equals 方法。
2. 【强制】禁止使用 new BigDecimal(double) 和 new BigDecimal(float) 构造函数。


# 其他
1. 业务代码不可使用自定义的rpc接口

# CR中体现的问题
1. 禁止在for循环中操作数据库/redis/调用接口，尤其是大数据量的循环 ，这可能会导致连接等资源大量消耗
2. 事务注解@Transactional需要指定rollbackfor异常类型为Exception.class，否则spring只会为RuntimeException进行回滚
3. 尽量将多次或者嵌套for循环改为map实现，否则容易出现性能问题

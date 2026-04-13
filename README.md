# db-compare-runnable

涓€涓熀浜?Spring Boot + Gradle 鐨勬暟鎹簱缁撴瀯姣斿宸ュ叿锛屾敮鎸佸涓簮搴撲笌涓€涓洰鏍囧簱涔嬮棿鐨?schema銆乼able銆乿iew銆乧olumn 缁撴瀯姣斿銆?
褰撳墠鏀寔涓ょ鎵ц妯″紡锛?
- `FULL_SCAN`锛氬厛鍔犺浇婧愬簱瀹屾暣鍏冩暟鎹紝鍐嶄笌鐩爣搴撳仛缁撴瀯姣斿銆?- `TARGET_DRIVEN`锛氫互鐩爣搴撳璞′负椹卞姩锛屽彧鎸変换鍔￠€愬紶鍔犺浇婧愯〃锛岄伩鍏嶅叏搴撴壂鎻忓甫鏉ョ殑楂樺唴瀛樺崰鐢ㄣ€?
褰撳墠榛樿閰嶇疆锛?
- `dbcompare.mode=TARGET_DRIVEN`
- `dbcompare.target.view-only=true`

## 褰撳墠鑳藉姏

- 瀵规瘮澶氫釜婧愬簱鍜屼竴涓洰鏍囧簱鐨?schema / table / column 缁撴瀯
- 鐩爣椹卞姩妯″紡涓嬫寜鍗曡〃绮惧噯鍔犺浇婧愬簱鍏冩暟鎹?- 杩囨护鏁版嵁搴撶郴缁熻嚜甯?schema锛屽彧姣斿鐢ㄦ埛瀵硅薄
- 鏀寔 AS400 library list 瑙ｆ瀽
- 杈撳嚭宸紓 CSV
- 杈撳嚭鍏ㄩ噺 Excel 鏄庣粏
- 杈撳嚭绠＄悊瑙嗚鐨勬眹鎬?Excel
- 杈撳嚭鍙洿鎺ュ鍏ユ暟鎹簱鐨?SQL 鏄庣粏

## 杩愯鏂瑰紡

椤圭洰榛樿浠?`src/main/resources/application.properties` 璇诲彇閰嶇疆銆?
甯哥敤鍛戒护锛?
```bash
./gradlew bootRun
./gradlew test
./gradlew clean build
```

Windows锛?
```bat
gradlew.bat bootRun
gradlew.bat test
gradlew.bat clean build
```

鍦?IDEA 涓彲鐩存帴杩愯锛?
- `src/main/java/com/example/dbcompare/app/CompareApplication.java`

## 椹卞姩渚濊禆

褰撳墠 Gradle 宸插唴缃互涓?JDBC 椹卞姩渚濊禆锛?
- DB2锛歚com.ibm.db2:jcc:11.5.9.0`
- AS400锛歚net.sf.jt400:jt400:21.0.6`
- openGauss / PostgreSQL锛歚org.postgresql:postgresql:42.7.3`

濡傛灉浣犲湪閰嶇疆鏂囦欢閲屾樉寮忔寚瀹氶┍鍔ㄧ被锛屽彲浣跨敤锛?
- DB2锛歚com.ibm.db2.jcc.DB2Driver`
- AS400锛歚com.ibm.as400.access.AS400JDBCDriver`
- openGauss锛歚org.postgresql.Driver`

## 閰嶇疆璇存槑

涓婚厤缃枃浠讹細

- `src/main/resources/application.properties`

涓昏閰嶇疆鍒嗙粍锛?
- `dbcompare.mode`锛氭墽琛屾ā寮忥紝鏀寔 `FULL_SCAN` / `TARGET_DRIVEN`
- `dbcompare.sources[n]`锛氭簮搴撻厤缃?- `dbcompare.target`锛氱洰鏍囧簱閰嶇疆
- `dbcompare.mappings[n]`锛氭簮搴撳埌鐩爣 schema 鐨勬槧灏?- `dbcompare.table-mappings[n]`锛氳〃绾ц鐩栨槧灏?- `dbcompare.options.*`锛氭瘮瀵归€夐」
- `dbcompare.output.*`锛氳緭鍑洪厤缃?
閲嶇偣閰嶇疆椤癸細

- `dbcompare.sources[n].driver-class-name`
  - 婧愬簱 JDBC 椹卞姩绫诲悕
- `dbcompare.target.driver-class-name`
  - 鐩爣搴?JDBC 椹卞姩绫诲悕
- `dbcompare.target.view-only=true`
  - 鐩爣椹卞姩妯″紡涓嬪彧鍔犺浇鐩爣 view
- `dbcompare.sources[n].schema`
  - 鎸囧畾婧愬簱榛樿 schema锛屼究浜庡崟琛ㄧ簿鍑嗘煡璇?- `dbcompare.output.csv-path`
- `dbcompare.output.excel-path`
- `dbcompare.output.summary-excel-path`
- `dbcompare.output.sql-path`
- `dbcompare.output.sql-table-name`
- `dbcompare.output.summary-path`

鍏煎璇存槑锛?
- 鏍囧噯閰嶇疆椤规槸 `driver-class-name`
- 鏃ч厤缃噷濡傛灉璇啓鎴?`drive-class-name`锛岀▼搴忎粛浼氬吋瀹硅瘑鍒?
## Target-Driven 妯″紡

`TARGET_DRIVEN` 妯″紡鐨勬牳蹇冩祦绋嬶細

1. 鍔犺浇鐩爣搴撶殑 view 鍏冩暟鎹?2. 鏍规嵁鐩爣 schema 鍙嶅悜瀹氫綅婧愬簱
3. 鏍规嵁琛ㄧ骇 mapping 鎴?view 鍚嶈В鏋愬嚭婧愯〃鍚?4. 瀵规簮搴撴墽琛屽崟琛ㄥ厓鏁版嵁鍔犺浇
5. 鎵ц缁撴瀯姣斿骞惰緭鍑虹粨鏋?
杩欎釜妯″紡涓昏鐢ㄤ簬锛?
- 閬垮厤涓€娆℃€у姞杞藉涓簮搴撶殑鍏ㄩ儴琛ㄧ粨鏋?- 闄嶄綆澶氭簮搴撳満鏅笅鐨勫爢鍐呭瓨鍗犵敤
- 鍙仛鐒︾湡瀹炲弬涓庢槧灏勭殑瀵硅薄

## AS400 鐗规畩澶勭悊

褰撴簮搴撶被鍨嬫槸 `AS400` 涓旀湭鏄惧紡鎻愪緵 schema 鏃讹細

- 绯荤粺浼氶€氳繃 `QSYS2.SYSTABLES` 涓?`QSYS2.LIBRARY_LIST_INFO` 鎸?library list 椤哄簭瑙ｆ瀽瀹為檯 schema
- 鑻ヨВ鏋愬埌澶氫釜鍊欓€?schema锛屼細杈撳嚭 `SOURCE_TABLE_AMBIGUOUS` 宸紓

## 瀵规瘮瑙勫垯

褰撳墠涓昏姣斿椤癸細

- 琛ㄦ槸鍚﹀瓨鍦?- 瀛楁鏄惁瀛樺湪
- 瀛楁绫诲瀷鏄惁涓€鑷?- 瀛楁闀垮害 / 绮惧害鏄惁涓€鑷?- 榛樿鍊兼槸鍚︿竴鑷?- nullable 鏄惁涓€鑷?
瀵硅薄绫诲瀷瑙勫垯锛?
- 婧愬簱鍥哄畾鎸?`TABLE` 鍔犺浇
- 鐩爣搴撳彲鎸夐厤缃€夋嫨 `TABLE` 鎴?`VIEW`
- 鍦?`TARGET_DRIVEN + target.view-only=true` 涓嬶紝鐩爣搴撴寜 `VIEW` 椹卞姩

绯荤粺 schema 杩囨护锛?
- DB2锛氬 `SYSCAT`銆乣SYSIBM`銆乣SYSTOOLS`
- openGauss锛氬 `pg_*`銆乣information_schema`
- AS400锛氬 `QSYS`銆乣QSYS2`銆乣QGPL`

## 鎶ヨ〃璇存槑

### CSV

CSV 鍙緭鍑哄樊寮傞」銆?
### Detail Excel

鏄庣粏 Excel 杈撳嚭鍏ㄩ噺瀛楁鏄庣粏锛屽寘鎷竴鑷撮」鍜屼笉涓€鑷撮」銆?
鐘舵€佸垪鍖呮嫭锛?
- `MATCH`
- `MISMATCH`
- `NOT_APPLICABLE`

褰撳崟涓?sheet 瓒呰繃 `.xlsx` 琛屾暟涓婇檺鏃讹紝浼氳嚜鍔ㄦ媶鍒嗕负锛?
- `Detail`
- `Detail_2`
- `Detail_3`
- ...

### Summary Excel

姹囨€?Excel 鐢ㄤ簬姹囨姤鍜岄闄╁垎鏋愶紝浼氳緭鍑哄涓?sheet锛?
- `Overview`
- `Diff Summary`
- `Risk Summary`
- `Schema Distribution`
- `Top Issue Tables`
- `Detail`

鍏朵腑锛?
- `Overview`锛氭€昏〃鏁般€佸凡鍖归厤琛ㄦ暟銆佹湭鍖归厤琛ㄦ暟銆佽鐩栫巼
- `Diff Summary`锛氭寜琛ㄧ骇涓诲樊寮傚垎绫荤粺璁?- `Risk Summary`锛氭寜浣?涓?楂橀闄╃粺璁?- `Schema Distribution`锛氭寜 schema 缁熻琛ㄦ暟閲?- `Top Issue Tables`锛氭寜宸紓鏁伴噺鎺掑簭鐨勯棶棰樿〃
- `Detail`锛氭寜 `source_db / schema / table / column / diff_type / detail` 杈撳嚭绠€鍖栨槑缁?
褰撳墠姹囨€昏鍒欙細

- 浠ヨ〃涓虹粺璁″崟浣?- 鍙琛ㄥ唴浠绘剰瀛楁瀛樺湪宸紓锛屽氨瑙嗕负闂琛?- 椋庨櫓鍒嗙骇榛樿瑙勫垯锛?  - 鏃犲樊寮傦細浣庨闄?  - 闀垮害 / 榛樿鍊?/ nullable 宸紓锛氫腑椋庨櫓
  - 缂哄瓧娈?/ 缂鸿〃 / 绫诲瀷涓嶄竴鑷?/ 姝т箟琛細楂橀闄?
### SQL

SQL 杈撳嚭涓庢槑缁?Excel 淇濇寔鍚屼竴濂楀垪缁撴瀯锛屼絾褰撳墠瀵煎嚭鑴氭湰宸茬粡鎸?MySQL 瀵煎叆鍦烘櫙鍋氫簡浼樺寲銆?
鍐呭鍖呮嫭锛?
- MySQL 鍙嬪ソ鐨?`DROP TABLE IF EXISTS`
- MySQL 鍙嬪ソ鐨?`CREATE TABLE IF NOT EXISTS`
- 鎵归噺 `INSERT INTO ... VALUES (...), (...)`

褰撳墠 SQL 瀵煎嚭閽堝 MySQL 鍋氫簡杩欎簺浼樺寲锛?
- 鏍囪瘑绗︿娇鐢ㄥ弽寮曞彿 `` ` ``锛屼笉渚濊禆 ANSI 鍙屽紩鍙锋ā寮?- 甯冨皵鍒椾娇鐢?`TINYINT(1)`
- 闀挎枃鏈垪浣跨敤 `TEXT`锛岄檷浣庡缓琛ㄦ椂鐨勮闀垮帇鍔?- 澶氭潯鏄庣粏浼氭寜鎵规鍚堝苟鎴愪竴鏉?`INSERT`锛屽噺灏戞枃浠朵綋绉拰瀵煎叆鑰楁椂
- 鑷姩杞箟鍙嶆枩鏉犮€佹崲琛屻€佸埗琛ㄧ鍜屽崟寮曞彿

榛樿琛ㄥ悕锛?
- `db_compare_detail_result`

榛樿杈撳嚭鏂囦欢锛?
- `build/reports/default-compare-detail.sql`

## 榛樿杈撳嚭鏂囦欢

榛樿浼氱敓鎴愶細

- `build/reports/default-compare-report.csv`
- `build/reports/default-compare-detail.xlsx`
- `build/reports/default-compare-summary.xlsx`
- `build/reports/default-compare-detail.sql`
- `build/reports/default-compare-summary.txt`

## 绀轰緥鏂囦欢

绀轰緥鐩綍锛?
- `examples/demo`
- `examples/sql`

鍏朵腑锛?
- `examples/demo`锛氫繚鐣?`FULL_SCAN` 鐨?snapshot 鍥炲綊鏁版嵁
- `examples/sql`锛欴B2 / openGauss 绀轰緥寤鸿〃鍜屾祴璇曟暟鎹剼鏈?
## 椤圭洰缁撴瀯

涓昏鐩綍锛?
- `src/main/java`锛氭牳蹇冧唬鐮?- `src/main/resources`锛歋pring Boot 閰嶇疆
- `src/test/java`锛氭祴璇曚唬鐮?- `examples/sql`锛氱ず渚?SQL
- `scripts`锛氱畝鍗曡剼鏈皝瑁?- `db_compare_requirements.md`锛氱洰鏍囬┍鍔ㄩ渶姹傛枃妗?- `db_compare_excel_requirements.md`锛氭眹鎬?Excel 闇€姹傛枃妗?
鏍稿績鍏ュ彛锛?
- `src/main/java/com/example/dbcompare/app/CompareApplication.java`

## 娴嬭瘯瑕嗙洊

褰撳墠宸茶鐩栵細

- 鐩爣椹卞姩缂栨帓妯″紡
- View 鍚嶈В鏋?- 鍙嶅悜 mapping
- 鏂伴厤缃」瑙ｆ瀽
- Excel / SQL 瀵煎嚭鑳藉姏
- 姹囨€?Excel 澶?sheet 杈撳嚭鑳藉姏
- 椹卞姩閰嶇疆鍏煎瑙ｆ瀽
- MySQL 鍙嬪ソ SQL 瀵煎嚭鑳藉姏

## 鍚庣画鍙户缁寮虹殑鏂瑰悜

- 浠庣洰鏍?View definition 涓洿瀹屾暣鍦拌В鏋愮湡瀹?base table
- 閽堝 DB2 / AS400 鐨勫崟琛ㄦ煡璇㈣繘涓€姝ュ仛鏁版嵁搴撲笓鐢?SQL 浼樺寲
- 灏?target-driven 浠诲姟瑙勫垝缁撴灉杈撳嚭涓哄崟鐙璁℃姤琛?- 瀵硅秴澶х粨鏋滈泦鎸?schema 鎴?source database 鑷姩鎷嗗垎杈撳嚭鏂囦欢
- 鏀寔瀵规眹鎬?Excel 鐨勯闄╄鍒欏拰 Top N 琛屾暟杩涜閰嶇疆鍖
param(
    [string]$OutputDir = "examples/demo-final-large",
    [int]$SchemaCount = 12,
    [int]$ViewsPerSchema = 18,
    [int]$Seed = 20260423
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function New-ColumnRow {
    param(
        [string]$DatabaseName,
        [string]$SchemaName,
        [string]$TableName,
        [string]$ColumnName,
        [string]$DataType,
        [string]$Length,
        [string]$Nullable,
        [string]$DefaultValue,
        [int]$OrdinalPosition
    )

    [pscustomobject]@{
        databaseName    = $DatabaseName
        schemaName      = $SchemaName
        tableName       = $TableName
        columnName      = $ColumnName
        dataType        = $DataType
        length          = $Length
        nullable        = $Nullable
        defaultValue    = $DefaultValue
        ordinalPosition = $OrdinalPosition
    }
}

function Get-SourceColumns {
    param([int]$TemplateIndex)

    switch ($TemplateIndex % 4) {
        0 {
            return @(
                @{ columnName = "ID";            dataType = "INTEGER"; length = "10";  nullable = "NO";  defaultValue = "" },
                @{ columnName = "BIZ_CODE";      dataType = "VARCHAR"; length = "16";  nullable = "NO";  defaultValue = "" },
                @{ columnName = "NAME";          dataType = "VARCHAR"; length = "60";  nullable = "YES"; defaultValue = "" },
                @{ columnName = "STATUS";        dataType = "CHAR";    length = "1";   nullable = "YES"; defaultValue = "A" },
                @{ columnName = "CREATED_AT";    dataType = "DATE";    length = "10";  nullable = "NO";  defaultValue = "" },
                @{ columnName = "LEGACY_FLAG";   dataType = "CHAR";    length = "1";   nullable = "YES"; defaultValue = "" },
                @{ columnName = "INTERNAL_NOTE"; dataType = "VARCHAR"; length = "120"; nullable = "YES"; defaultValue = "" }
            )
        }
        1 {
            return @(
                @{ columnName = "ID";             dataType = "BIGINT";  length = "19";  nullable = "NO";  defaultValue = "" },
                @{ columnName = "CUSTOMER_NO";    dataType = "CHAR";    length = "12";  nullable = "NO";  defaultValue = "" },
                @{ columnName = "FULL_NAME";      dataType = "VARCHAR"; length = "80";  nullable = "YES"; defaultValue = "" },
                @{ columnName = "LEVEL_CODE";     dataType = "VARCHAR"; length = "8";   nullable = "YES"; defaultValue = "L1" },
                @{ columnName = "OPEN_DATE";      dataType = "DATE";    length = "10";  nullable = "NO";  defaultValue = "" },
                @{ columnName = "SYNC_TAG";       dataType = "CHAR";    length = "1";   nullable = "YES"; defaultValue = "" },
                @{ columnName = "OPERATOR_REMARK";dataType = "VARCHAR"; length = "200"; nullable = "YES"; defaultValue = "" }
            )
        }
        2 {
            return @(
                @{ columnName = "ID";             dataType = "DECIMAL"; length = "18";  nullable = "NO";  defaultValue = "" },
                @{ columnName = "REGION_CODE";    dataType = "CHAR";    length = "6";   nullable = "NO";  defaultValue = "" },
                @{ columnName = "OWNER_NAME";     dataType = "VARCHAR"; length = "50";  nullable = "YES"; defaultValue = "" },
                @{ columnName = "AMOUNT";         dataType = "DECIMAL"; length = "12,2";nullable = "NO";  defaultValue = "0" },
                @{ columnName = "UPDATED_AT";     dataType = "DATE";    length = "10";  nullable = "NO";  defaultValue = "" },
                @{ columnName = "HISTORY_MARK";   dataType = "CHAR";    length = "1";   nullable = "YES"; defaultValue = "" },
                @{ columnName = "MANUAL_COMMENT"; dataType = "VARCHAR"; length = "180"; nullable = "YES"; defaultValue = "" }
            )
        }
        default {
            return @(
                @{ columnName = "ID";            dataType = "INTEGER"; length = "10";  nullable = "NO";  defaultValue = "" },
                @{ columnName = "CATEGORY";      dataType = "VARCHAR"; length = "24";  nullable = "YES"; defaultValue = "" },
                @{ columnName = "SCORE";         dataType = "INTEGER"; length = "10";  nullable = "YES"; defaultValue = "0" },
                @{ columnName = "ACTIVE_FLAG";   dataType = "CHAR";    length = "1";   nullable = "NO";  defaultValue = "Y" },
                @{ columnName = "LAST_SYNC_AT";  dataType = "DATE";    length = "10";  nullable = "NO";  defaultValue = "" },
                @{ columnName = "SOURCE_TRACE";  dataType = "VARCHAR"; length = "40";  nullable = "YES"; defaultValue = "" },
                @{ columnName = "ARCHIVE_NOTE";  dataType = "VARCHAR"; length = "160"; nullable = "YES"; defaultValue = "" }
            )
        }
    }
}

function Clone-Columns {
    param([object[]]$Columns)

    $cloned = @()
    foreach ($column in $Columns) {
        $cloned += ,@{
                columnName   = $column.columnName
                dataType     = $column.dataType
                length       = $column.length
                nullable     = $column.nullable
                defaultValue = $column.defaultValue
            }
    }
    return $cloned
}

function Get-ViewColumns {
    param(
        [object[]]$SourceColumns,
        [int]$ScenarioCode
    )

    $columns = @(Clone-Columns -Columns $SourceColumns)

    switch ($ScenarioCode % 12) {
        0 {
            return $columns
        }
        1 {
            $columns[2].length = [string]([int]$columns[2].length + 20)
            return $columns
        }
        2 {
            $columns = @($columns | Where-Object { $_.columnName -ne "STATUS" -and $_.columnName -ne "LEVEL_CODE" -and $_.columnName -ne "ACTIVE_FLAG" })
            $columns += @{
                columnName   = "TARGET_ONLY_FLAG"
                dataType     = "VARCHAR"
                length       = "8"
                nullable     = "YES"
                defaultValue = ""
            }
            return $columns
        }
        3 {
            $columns = @($columns | Where-Object { $_.columnName -notin @("LEGACY_FLAG", "INTERNAL_NOTE", "SYNC_TAG", "OPERATOR_REMARK", "HISTORY_MARK", "MANUAL_COMMENT", "SOURCE_TRACE", "ARCHIVE_NOTE") })
            return $columns
        }
        4 {
            $columns[4].dataType = "TIMESTAMP"
            $columns = @($columns | Where-Object { $_.columnName -notin @("LEGACY_FLAG", "SYNC_TAG", "HISTORY_MARK", "SOURCE_TRACE") })
            return $columns
        }
        5 {
            $columns[1].dataType = "NUMERIC"
            $columns[1].length = "18"
            return $columns
        }
        6 {
            $columns[2].length = [string]([int]$columns[2].length + 10)
            $columns = @($columns | Where-Object { $_.columnName -notin @("INTERNAL_NOTE", "OPERATOR_REMARK", "MANUAL_COMMENT", "ARCHIVE_NOTE") })
            return $columns
        }
        7 {
            $columns = @($columns | Where-Object { $_.columnName -notin @("LEGACY_FLAG", "SYNC_TAG", "HISTORY_MARK", "SOURCE_TRACE") })
            $columns += @{
                columnName   = "TARGET_ONLY_STATUS"
                dataType     = "VARCHAR"
                length       = "12"
                nullable     = "YES"
                defaultValue = ""
            }
            $columns[2].length = [string]([int]$columns[2].length + 15)
            return $columns
        }
        8 {
            $columns[0].dataType = "NUMERIC"
            $columns[0].length = "18"
            $columns[3].dataType = "VARCHAR"
            $columns[3].length = "12"
            return $columns
        }
        9 {
            $columns[2].length = [string]([int]$columns[2].length - 8)
            $columns[4].dataType = "TIMESTAMP"
            return $columns
        }
        10 {
            $columns = @($columns | Where-Object { $_.columnName -notin @("LEGACY_FLAG", "SYNC_TAG", "HISTORY_MARK", "SOURCE_TRACE", "INTERNAL_NOTE", "OPERATOR_REMARK") })
            $columns += @{
                columnName   = "TARGET_ONLY_REMARK"
                dataType     = "TEXT"
                length       = "500"
                nullable     = "YES"
                defaultValue = ""
            }
            return $columns
        }
        default {
            $columns[1].dataType = "VARCHAR"
            $columns[1].length = [string]([int]$columns[1].length + 4)
            $columns[2].dataType = "TEXT"
            $columns[2].length = "500"
            $columns = @($columns | Where-Object { $_.columnName -notin @("LEGACY_FLAG", "SOURCE_TRACE") })
            return $columns
        }
    }
}

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$outputPath = Join-Path $root $OutputDir
New-Item -ItemType Directory -Force -Path $outputPath | Out-Null

$sourceRows = New-Object System.Collections.Generic.List[object]
$targetRows = New-Object System.Collections.Generic.List[object]
$lineageRows = New-Object System.Collections.Generic.List[object]
$schemaMappings = New-Object System.Collections.Generic.List[object]
$tableMappings = New-Object System.Collections.Generic.List[object]

$sourceName = "DB2_A"
$random = [System.Random]::new($Seed)

for ($schemaIndex = 1; $schemaIndex -le $SchemaCount; $schemaIndex++) {
    $sourceSchema = "LEGACY_{0:D2}" -f $schemaIndex
    $targetViewSchema = "VIEW_APP_{0:D2}" -f $schemaIndex
    $schemaMappings.Add([pscustomobject]@{
        sourceDatabaseName = $sourceName
        targetSchemaName   = $targetViewSchema
    })

    for ($viewOffset = 1; $viewOffset -le $ViewsPerSchema; $viewOffset++) {
        $globalIndex = (($schemaIndex - 1) * $ViewsPerSchema) + $viewOffset
        $sourceTable = "BIZ_TABLE_{0:D3}" -f $globalIndex
        $targetView = "{0}_VIEW" -f $sourceTable
        $sourceColumns = @(Get-SourceColumns -TemplateIndex $globalIndex)

        for ($columnIndex = 0; $columnIndex -lt $sourceColumns.Count; $columnIndex++) {
            $column = $sourceColumns[$columnIndex]
            $sourceRows.Add((New-ColumnRow -DatabaseName $sourceName -SchemaName $sourceSchema -TableName $sourceTable `
                    -ColumnName $column.columnName -DataType $column.dataType -Length $column.length `
                    -Nullable $column.nullable -DefaultValue $column.defaultValue -OrdinalPosition ($columnIndex + 1)))
        }

        $scenarioCode = $random.Next(0, 12)
        $targetColumns = @(Get-ViewColumns -SourceColumns $sourceColumns -ScenarioCode $scenarioCode)
        for ($columnIndex = 0; $columnIndex -lt $targetColumns.Count; $columnIndex++) {
            $column = $targetColumns[$columnIndex]
            $targetRows.Add((New-ColumnRow -DatabaseName "GAUSS_FINAL" -SchemaName $targetViewSchema -TableName $targetView `
                    -ColumnName $column.columnName -DataType $column.dataType -Length $column.length `
                    -Nullable $column.nullable -DefaultValue $column.defaultValue -OrdinalPosition ($columnIndex + 1)))
        }

        $lineageRows.Add([pscustomobject]@{
            targetViewSchema  = $targetViewSchema
            targetView        = $targetView
            targetTableSchema = "ODS_{0:D2}" -f $schemaIndex
            targetTable       = "{0}_BASE" -f $sourceTable
        })
        $lineageRows.Add([pscustomobject]@{
            targetViewSchema  = $targetViewSchema
            targetView        = $targetView
            targetTableSchema = "DWD_{0:D2}" -f $schemaIndex
            targetTable       = "{0}_DIM" -f $sourceTable
        })
        if ($random.NextDouble() -lt 0.45) {
            $lineageRows.Add([pscustomobject]@{
                targetViewSchema  = $targetViewSchema
                targetView        = $targetView
                targetTableSchema = "ADS_{0:D2}" -f $schemaIndex
                targetTable       = "{0}_SUMMARY" -f $sourceTable
            })
        }
        if ($random.NextDouble() -lt 0.20) {
            $lineageRows.Add([pscustomobject]@{
                targetViewSchema  = $targetViewSchema
                targetView        = $targetView
                targetTableSchema = "DM_{0:D2}" -f $schemaIndex
                targetTable       = "{0}_WIDE" -f $sourceTable
            })
        }

        $tableMappings.Add([pscustomobject]@{
            sourceDatabaseName = $sourceName
            sourceSchemaName   = $sourceSchema
            sourceTableName    = $sourceTable
            targetSchemaName   = $targetViewSchema
            targetTableName    = $targetView
        })
    }
}

$sourceRows | Export-Csv -Path (Join-Path $outputPath "source_db2_a_large.csv") -NoTypeInformation -Encoding UTF8
$targetRows | Export-Csv -Path (Join-Path $outputPath "target_views_large.csv") -NoTypeInformation -Encoding UTF8
$lineageRows | Export-Csv -Path (Join-Path $outputPath "target_view_lineage_large.csv") -NoTypeInformation -Encoding UTF8

$propertiesLines = New-Object System.Collections.Generic.List[string]
$propertiesLines.Add("dbcompare.mode=TARGET_DRIVEN")
$propertiesLines.Add("")
$propertiesLines.Add("dbcompare.sources[0].source-name=$sourceName")
$propertiesLines.Add("dbcompare.sources[0].type=SNAPSHOT")
$propertiesLines.Add("dbcompare.sources[0].snapshot-file=$OutputDir/source_db2_a_large.csv")
$propertiesLines.Add("")
$propertiesLines.Add("dbcompare.target.source-name=GAUSS_FINAL")
$propertiesLines.Add("dbcompare.target.type=SNAPSHOT")
$propertiesLines.Add("dbcompare.target.view-only=true")
$propertiesLines.Add("dbcompare.target.snapshot-file=$OutputDir/target_views_large.csv")
$propertiesLines.Add("dbcompare.target.view-lineage-file=$OutputDir/target_view_lineage_large.csv")
$propertiesLines.Add("")
for ($i = 0; $i -lt $schemaMappings.Count; $i++) {
    $mapping = $schemaMappings[$i]
    $propertiesLines.Add("dbcompare.mappings[$i].source-database-name=$($mapping.sourceDatabaseName)")
    $propertiesLines.Add("dbcompare.mappings[$i].target-schema-name=$($mapping.targetSchemaName)")
}
$propertiesLines.Add("")
for ($i = 0; $i -lt $tableMappings.Count; $i++) {
    $mapping = $tableMappings[$i]
    $propertiesLines.Add("dbcompare.table-mappings[$i].source-database-name=$($mapping.sourceDatabaseName)")
    $propertiesLines.Add("dbcompare.table-mappings[$i].source-schema-name=$($mapping.sourceSchemaName)")
    $propertiesLines.Add("dbcompare.table-mappings[$i].source-table-name=$($mapping.sourceTableName)")
    $propertiesLines.Add("dbcompare.table-mappings[$i].target-schema-name=$($mapping.targetSchemaName)")
    $propertiesLines.Add("dbcompare.table-mappings[$i].target-table-name=$($mapping.targetTableName)")
}
$propertiesLines.Add("")
$propertiesLines.Add("dbcompare.options.compare-exists=true")
$propertiesLines.Add("dbcompare.options.compare-type=true")
$propertiesLines.Add("dbcompare.options.compare-length=true")
$propertiesLines.Add("dbcompare.options.compare-default-value=false")
$propertiesLines.Add("dbcompare.options.compare-nullable=false")
$propertiesLines.Add("dbcompare.options.object-type=VIEW")
$propertiesLines.Add("dbcompare.options.relation-mode=TABLE_TO_VIEW")
$propertiesLines.Add("dbcompare.options.source-load-threads=2")
$propertiesLines.Add("dbcompare.options.type-mappings.date=DATE,TIMESTAMP")
$propertiesLines.Add("dbcompare.options.type-mappings.int=INT,INTEGER,NUMBER")
$propertiesLines.Add("")
$propertiesLines.Add("dbcompare.output.csv-path=build/reports/demo-final-large-report.csv")
$propertiesLines.Add("dbcompare.output.excel-path=build/reports/demo-final-large-detail.xlsx")
$propertiesLines.Add("dbcompare.output.summary-excel-path=build/reports/demo-final-large-summary.xlsx")
$propertiesLines.Add("dbcompare.output.target-view-lineage-excel-path=build/reports/demo-final-large-target-view-lineage.xlsx")
$propertiesLines.Add("dbcompare.output.sql-path=build/reports/demo-final-large-detail.sql")
$propertiesLines.Add("dbcompare.output.sql-table-name=db_compare_detail_result")
$propertiesLines.Add("dbcompare.output.summary-path=build/reports/demo-final-large-summary.txt")

[System.IO.File]::WriteAllLines(
    (Join-Path $outputPath "demo-final-large.properties"),
    $propertiesLines,
    [System.Text.UTF8Encoding]::new($false)
)

Write-Host "Generated large final demo in $outputPath"
Write-Host ("Schemas={0}, views={1}, lineageRows={2}" -f $SchemaCount, ($SchemaCount * $ViewsPerSchema), $lineageRows.Count)

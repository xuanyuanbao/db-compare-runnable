param(
    [string]$OutputDir = "examples/demo-view-lineage-large",
    [int]$SchemaCount = 12,
    [int]$TablesPerSchema = 12
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
                @{ columnName = "ID";         dataType = "INTEGER";   length = "10";   nullable = "NO";  defaultValue = ""  },
                @{ columnName = "CODE";       dataType = "VARCHAR";   length = "16";   nullable = "NO";  defaultValue = ""  },
                @{ columnName = "NAME";       dataType = "VARCHAR";   length = "60";   nullable = "YES"; defaultValue = ""  },
                @{ columnName = "STATUS";     dataType = "CHAR";      length = "1";    nullable = "NO";  defaultValue = "A" },
                @{ columnName = "AMOUNT";     dataType = "DECIMAL";   length = "12,2"; nullable = "NO";  defaultValue = "0" },
                @{ columnName = "UPDATED_AT"; dataType = "TIMESTAMP"; length = "";     nullable = "YES"; defaultValue = ""  }
            )
        }
        1 {
            return @(
                @{ columnName = "ID";          dataType = "BIGINT";    length = "19";   nullable = "NO";  defaultValue = ""  },
                @{ columnName = "BIZ_CODE";    dataType = "CHAR";      length = "8";    nullable = "NO";  defaultValue = ""  },
                @{ columnName = "DESCRIPTION"; dataType = "VARCHAR";   length = "120";  nullable = "YES"; defaultValue = ""  },
                @{ columnName = "FLAG";        dataType = "CHAR";      length = "1";    nullable = "YES"; defaultValue = "N" },
                @{ columnName = "VERSION";     dataType = "INTEGER";   length = "10";   nullable = "NO";  defaultValue = "1" },
                @{ columnName = "CREATED_AT";  dataType = "TIMESTAMP"; length = "";     nullable = "NO";  defaultValue = ""  }
            )
        }
        2 {
            return @(
                @{ columnName = "ID";           dataType = "DECIMAL";   length = "18";   nullable = "NO";  defaultValue = ""  },
                @{ columnName = "REGION_CODE";  dataType = "CHAR";      length = "6";    nullable = "NO";  defaultValue = ""  },
                @{ columnName = "OWNER_NAME";   dataType = "VARCHAR";   length = "50";   nullable = "YES"; defaultValue = ""  },
                @{ columnName = "REMARK";       dataType = "VARCHAR";   length = "200";  nullable = "YES"; defaultValue = ""  },
                @{ columnName = "LAST_SYNC_AT"; dataType = "TIMESTAMP"; length = "";     nullable = "YES"; defaultValue = ""  },
                @{ columnName = "ACTIVE";       dataType = "CHAR";      length = "1";    nullable = "NO";  defaultValue = "Y" }
            )
        }
        default {
            return @(
                @{ columnName = "ID";          dataType = "INTEGER";   length = "10";   nullable = "NO";  defaultValue = ""  },
                @{ columnName = "CATEGORY";    dataType = "VARCHAR";   length = "24";   nullable = "YES"; defaultValue = ""  },
                @{ columnName = "SCORE";       dataType = "INTEGER";   length = "10";   nullable = "YES"; defaultValue = "0" },
                @{ columnName = "OWNER_ID";    dataType = "VARCHAR";   length = "20";   nullable = "NO";  defaultValue = ""  },
                @{ columnName = "AUDIT_MARK";  dataType = "CHAR";      length = "1";    nullable = "YES"; defaultValue = ""  },
                @{ columnName = "CREATED_BY";  dataType = "VARCHAR";   length = "40";   nullable = "NO";  defaultValue = ""  }
            )
        }
    }
}

function Get-TargetColumns {
    param(
        [object[]]$SourceColumns,
        [int]$ScenarioIndex
    )

    $columns = foreach ($column in $SourceColumns) {
        @{
            columnName   = $column.columnName
            dataType     = $column.dataType
            length       = $column.length
            nullable     = $column.nullable
            defaultValue = $column.defaultValue
        }
    }

    switch ($ScenarioIndex % 9) {
        0 { return $columns }
        1 {
            $columns[2].length = "80"
            return $columns
        }
        2 {
            $columns[3].dataType = "VARCHAR"
            $columns[3].length = "4"
            return $columns
        }
        3 {
            $columns[4].defaultValue = if ($columns[4].defaultValue -eq "") { "AUTO" } else { "2" }
            return $columns
        }
        4 {
            $columns[1].nullable = if ($columns[1].nullable -eq "YES") { "NO" } else { "YES" }
            return $columns
        }
        5 {
            return $columns[0..($columns.Count - 2)]
        }
        6 {
            $columns += @{
                columnName   = "EXTRA_FLAG"
                dataType     = "VARCHAR"
                length       = "5"
                nullable     = "YES"
                defaultValue = ""
            }
            return $columns
        }
        7 {
            $columns[0].dataType = "NUMERIC"
            $columns[0].length = "18"
            $columns[2].length = "30"
            return $columns
        }
        default {
            $columns[5].nullable = if ($columns[5].nullable -eq "YES") { "NO" } else { "YES" }
            $columns[3].defaultValue = "ARCHIVED"
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
$mappings = New-Object System.Collections.Generic.List[object]
$tableMappings = New-Object System.Collections.Generic.List[object]

$sourceName = "DB2_A"

for ($schemaIndex = 1; $schemaIndex -le $SchemaCount; $schemaIndex++) {
    $sourceSchema = "LEGACY_{0:D2}" -f $schemaIndex
    $targetViewSchema = "VIEW_APP_{0:D2}" -f $schemaIndex
    $mappings.Add([pscustomobject]@{
        sourceDatabaseName = $sourceName
        targetSchemaName   = $targetViewSchema
    })

    for ($tableOffset = 1; $tableOffset -le $TablesPerSchema; $tableOffset++) {
        $globalIndex = (($schemaIndex - 1) * $TablesPerSchema) + $tableOffset
        $sourceTable = "TABLE_{0:D3}" -f $globalIndex
        $targetView = "{0}_VIEW" -f $sourceTable
        $templateColumns = @(Get-SourceColumns -TemplateIndex $globalIndex)
        for ($columnIndex = 0; $columnIndex -lt $templateColumns.Count; $columnIndex++) {
            $column = $templateColumns[$columnIndex]
            $sourceRows.Add((New-ColumnRow -DatabaseName $sourceName -SchemaName $sourceSchema -TableName $sourceTable `
                    -ColumnName $column.columnName -DataType $column.dataType -Length $column.length `
                    -Nullable $column.nullable -DefaultValue $column.defaultValue -OrdinalPosition ($columnIndex + 1)))
        }

        $targetColumns = @(Get-TargetColumns -SourceColumns $templateColumns -ScenarioIndex ($globalIndex - 1))
        for ($columnIndex = 0; $columnIndex -lt $targetColumns.Count; $columnIndex++) {
            $column = $targetColumns[$columnIndex]
            $targetRows.Add((New-ColumnRow -DatabaseName "GAUSS_VIEW" -SchemaName $targetViewSchema -TableName $targetView `
                    -ColumnName $column.columnName -DataType $column.dataType -Length $column.length `
                    -Nullable $column.nullable -DefaultValue $column.defaultValue -OrdinalPosition ($columnIndex + 1)))
        }

        $lineageRows.Add([pscustomobject]@{
            targetViewSchema = $targetViewSchema
            targetView       = $targetView
            targetTableSchema = "ODS_{0:D2}" -f $schemaIndex
            targetTable      = "{0}_BASE" -f $sourceTable
        })
        $lineageRows.Add([pscustomobject]@{
            targetViewSchema = $targetViewSchema
            targetView       = $targetView
            targetTableSchema = "DWD_{0:D2}" -f $schemaIndex
            targetTable      = "{0}_PROFILE" -f $sourceTable
        })
        if ($globalIndex % 3 -eq 0) {
            $lineageRows.Add([pscustomobject]@{
                targetViewSchema = $targetViewSchema
                targetView       = $targetView
                targetTableSchema = "ADS_{0:D2}" -f $schemaIndex
                targetTable      = "{0}_SUMMARY" -f $sourceTable
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
$propertiesLines.Add("mode=TARGET_DRIVEN")
$propertiesLines.Add("source.count=1")
$propertiesLines.Add("source.1.name=$sourceName")
$propertiesLines.Add("source.1.type=SNAPSHOT")
$propertiesLines.Add("source.1.snapshotFile=$OutputDir/source_db2_a_large.csv")
$propertiesLines.Add("")
$propertiesLines.Add("target.name=GAUSS_VIEW")
$propertiesLines.Add("target.type=SNAPSHOT")
$propertiesLines.Add("target.viewOnly=true")
$propertiesLines.Add("target.snapshotFile=$OutputDir/target_views_large.csv")
$propertiesLines.Add("target.viewLineageFile=$OutputDir/target_view_lineage_large.csv")
$propertiesLines.Add("")
$propertiesLines.Add("mapping.count=$($mappings.Count)")
for ($i = 0; $i -lt $mappings.Count; $i++) {
    $propertiesLines.Add("mapping.$($i + 1).sourceDatabaseName=$($mappings[$i].sourceDatabaseName)")
    $propertiesLines.Add("mapping.$($i + 1).targetSchemaName=$($mappings[$i].targetSchemaName)")
}
$propertiesLines.Add("")
$propertiesLines.Add("tableMapping.count=$($tableMappings.Count)")
for ($i = 0; $i -lt $tableMappings.Count; $i++) {
    $mapping = $tableMappings[$i]
    $propertiesLines.Add("tableMapping.$($i + 1).sourceDatabaseName=$($mapping.sourceDatabaseName)")
    $propertiesLines.Add("tableMapping.$($i + 1).sourceSchemaName=$($mapping.sourceSchemaName)")
    $propertiesLines.Add("tableMapping.$($i + 1).sourceTableName=$($mapping.sourceTableName)")
    $propertiesLines.Add("tableMapping.$($i + 1).targetSchemaName=$($mapping.targetSchemaName)")
    $propertiesLines.Add("tableMapping.$($i + 1).targetTableName=$($mapping.targetTableName)")
}
$propertiesLines.Add("")
$propertiesLines.Add("compare.options.compareNullable=true")
$propertiesLines.Add("compare.options.compareDefaultValue=true")
$propertiesLines.Add("compare.options.compareLength=true")
$propertiesLines.Add("compare.options.object-type=VIEW")
$propertiesLines.Add("compare.options.sourceLoadThreads=2")
$propertiesLines.Add("")
$propertiesLines.Add("output.csvPath=build/reports/demo-view-lineage-large-report.csv")
$propertiesLines.Add("output.excelPath=build/reports/demo-view-lineage-large-detail.xlsx")
$propertiesLines.Add("output.summaryExcelPath=build/reports/demo-view-lineage-large-summary.xlsx")
$propertiesLines.Add("output.targetViewLineageExcelPath=build/reports/demo-view-lineage-large-target-view-lineage.xlsx")
$propertiesLines.Add("output.sqlPath=build/reports/demo-view-lineage-large-detail.sql")
$propertiesLines.Add("output.sqlTableName=db_compare_detail_result")
$propertiesLines.Add("output.summaryPath=build/reports/demo-view-lineage-large-summary.txt")

[System.IO.File]::WriteAllLines(
    (Join-Path $outputPath "demo-view-lineage-large.properties"),
    $propertiesLines,
    [System.Text.UTF8Encoding]::new($false)
)

Write-Host "Generated target-view-lineage large demo in $outputPath"
Write-Host ("Schemas={0}, tables={1}, views={2}, lineageRows={3}" -f $SchemaCount, ($SchemaCount * $TablesPerSchema), ($SchemaCount * $TablesPerSchema), $lineageRows.Count)

param(
    [string]$OutputDir = "examples/demo-large",
    [int]$SourceCount = 12,
    [int]$TablesPerSource = 10
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

function Get-TemplateColumns {
    param([int]$TemplateIndex)

    switch ($TemplateIndex % 4) {
        0 {
            return @(
                @{ columnName = "ID";         dataType = "INTEGER";   length = "10";    nullable = "NO";  defaultValue = ""   },
                @{ columnName = "CODE";       dataType = "VARCHAR";   length = "16";    nullable = "NO";  defaultValue = ""   },
                @{ columnName = "NAME";       dataType = "VARCHAR";   length = "60";    nullable = "YES"; defaultValue = ""   },
                @{ columnName = "STATUS";     dataType = "CHAR";      length = "1";     nullable = "NO";  defaultValue = "A"  },
                @{ columnName = "AMOUNT";     dataType = "DECIMAL";   length = "12,2";  nullable = "NO";  defaultValue = "0"  },
                @{ columnName = "UPDATED_AT"; dataType = "TIMESTAMP"; length = "";      nullable = "YES"; defaultValue = ""   }
            )
        }
        1 {
            return @(
                @{ columnName = "ID";         dataType = "BIGINT";    length = "19";    nullable = "NO";  defaultValue = ""   },
                @{ columnName = "CATEGORY";   dataType = "VARCHAR";   length = "24";    nullable = "YES"; defaultValue = ""   },
                @{ columnName = "SCORE";      dataType = "INTEGER";   length = "10";    nullable = "YES"; defaultValue = "0"  },
                @{ columnName = "FLAG";       dataType = "CHAR";      length = "1";     nullable = "YES"; defaultValue = "N"  },
                @{ columnName = "CREATED_BY"; dataType = "VARCHAR";   length = "40";    nullable = "NO";  defaultValue = ""   },
                @{ columnName = "CREATED_AT"; dataType = "TIMESTAMP"; length = "";      nullable = "NO";  defaultValue = ""   }
            )
        }
        2 {
            return @(
                @{ columnName = "ID";          dataType = "DECIMAL";   length = "18";    nullable = "NO";  defaultValue = ""   },
                @{ columnName = "BIZ_CODE";    dataType = "CHAR";      length = "8";     nullable = "NO";  defaultValue = ""   },
                @{ columnName = "DESCRIPTION"; dataType = "VARCHAR";   length = "120";   nullable = "YES"; defaultValue = ""   },
                @{ columnName = "RATE";        dataType = "DECIMAL";   length = "8,4";   nullable = "YES"; defaultValue = "0"  },
                @{ columnName = "ACTIVE";      dataType = "CHAR";      length = "1";     nullable = "NO";  defaultValue = "Y"  },
                @{ columnName = "LAST_SYNC";   dataType = "TIMESTAMP"; length = "";      nullable = "YES"; defaultValue = ""   }
            )
        }
        default {
            return @(
                @{ columnName = "ID";          dataType = "INTEGER";   length = "10";    nullable = "NO";  defaultValue = ""   },
                @{ columnName = "REGION_CODE"; dataType = "CHAR";      length = "6";     nullable = "NO";  defaultValue = ""   },
                @{ columnName = "OWNER_NAME";  dataType = "VARCHAR";   length = "50";    nullable = "YES"; defaultValue = ""   },
                @{ columnName = "REMARK";      dataType = "VARCHAR";   length = "200";   nullable = "YES"; defaultValue = ""   },
                @{ columnName = "VERSION";     dataType = "INTEGER";   length = "10";    nullable = "NO";  defaultValue = "1"  },
                @{ columnName = "UPDATED_BY";  dataType = "VARCHAR";   length = "30";    nullable = "YES"; defaultValue = ""   }
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

    switch ($ScenarioIndex) {
        0 {
            return $columns
        }
        1 {
            $columns[4].dataType = "VARCHAR"
            $columns[4].length = "32"
            return $columns
        }
        2 {
            $columns[2].length = "90"
            return $columns
        }
        3 {
            $columns[3].defaultValue = if ($columns[3].defaultValue -eq "A") { "I" } elseif ($columns[3].defaultValue -eq "N") { "Y" } else { "Z" }
            return $columns
        }
        4 {
            $columns[2].nullable = if ($columns[2].nullable -eq "YES") { "NO" } else { "YES" }
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
            return @()
        }
        8 {
            $columns[1].dataType = "NUMERIC"
            $columns[1].length = "18"
            $columns[2].length = "30"
            return $columns
        }
        9 {
            $columns[3].defaultValue = "ARCHIVED"
            $columns[5].nullable = if ($columns[5].nullable -eq "YES") { "NO" } else { "YES" }
            $columns += @{
                columnName   = "AUDIT_MARK"
                dataType     = "CHAR"
                length       = "1"
                nullable     = "YES"
                defaultValue = ""
            }
            return $columns
        }
        default {
            return $columns
        }
    }
}

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$outputPath = Join-Path $root $OutputDir
New-Item -ItemType Directory -Force -Path $outputPath | Out-Null

$targetRows = New-Object System.Collections.Generic.List[object]
$sourceDefinitions = New-Object System.Collections.Generic.List[object]

for ($sourceIndex = 1; $sourceIndex -le $SourceCount; $sourceIndex++) {
    $sourceName = "SRC_{0:D2}" -f $sourceIndex
    $sourceSchema = "LEGACY_{0:D2}" -f $sourceIndex
    $targetSchema = "T_SRC_{0:D2}" -f $sourceIndex
    $sourceRows = New-Object System.Collections.Generic.List[object]

    for ($tableOffset = 1; $tableOffset -le $TablesPerSource; $tableOffset++) {
        $globalTableIndex = (($sourceIndex - 1) * $TablesPerSource) + $tableOffset
        $tableName = "TABLE_{0:D3}" -f $globalTableIndex
        $templateColumns = @(Get-TemplateColumns -TemplateIndex $globalTableIndex)

        for ($columnIndex = 0; $columnIndex -lt $templateColumns.Count; $columnIndex++) {
            $column = $templateColumns[$columnIndex]
            $sourceRows.Add((New-ColumnRow -DatabaseName $sourceName -SchemaName $sourceSchema -TableName $tableName `
                    -ColumnName $column.columnName -DataType $column.dataType -Length $column.length `
                    -Nullable $column.nullable -DefaultValue $column.defaultValue -OrdinalPosition ($columnIndex + 1)))
        }

        $scenarioIndex = ($globalTableIndex - 1) % 10
        $targetColumns = @(Get-TargetColumns -SourceColumns $templateColumns -ScenarioIndex $scenarioIndex)
        for ($columnIndex = 0; $columnIndex -lt $targetColumns.Count; $columnIndex++) {
            $column = $targetColumns[$columnIndex]
            $targetRows.Add((New-ColumnRow -DatabaseName "GAUSS_LARGE" -SchemaName $targetSchema -TableName $tableName `
                    -ColumnName $column.columnName -DataType $column.dataType -Length $column.length `
                    -Nullable $column.nullable -DefaultValue $column.defaultValue -OrdinalPosition ($columnIndex + 1)))
        }
    }

    $sourceFileName = ("source_{0}.csv" -f $sourceName.ToLowerInvariant())
    $sourceFilePath = Join-Path $outputPath $sourceFileName
    $sourceRows | Export-Csv -Path $sourceFilePath -NoTypeInformation -Encoding UTF8

    $sourceDefinitions.Add([pscustomobject]@{
        index        = $sourceIndex
        sourceName   = $sourceName
        sourceSchema = $sourceSchema
        targetSchema = $targetSchema
        sourceFile   = $sourceFileName
    })
}

$targetFilePath = Join-Path $outputPath "target_gauss_large.csv"
$targetRows | Export-Csv -Path $targetFilePath -NoTypeInformation -Encoding UTF8

$propertiesLines = New-Object System.Collections.Generic.List[string]
$propertiesLines.Add("mode=FULL_SCAN")
$propertiesLines.Add("source.count=$SourceCount")
foreach ($source in $sourceDefinitions) {
    $propertiesLines.Add("source.$($source.index).name=$($source.sourceName)")
    $propertiesLines.Add("source.$($source.index).type=SNAPSHOT")
    $propertiesLines.Add("source.$($source.index).snapshotFile=$OutputDir/$($source.sourceFile)")
}
$propertiesLines.Add("")
$propertiesLines.Add("target.name=GAUSS_LARGE")
$propertiesLines.Add("target.type=SNAPSHOT")
$propertiesLines.Add("target.snapshotFile=$OutputDir/target_gauss_large.csv")
$propertiesLines.Add("")
$propertiesLines.Add("mapping.count=$SourceCount")
foreach ($source in $sourceDefinitions) {
    $propertiesLines.Add("mapping.$($source.index).sourceDatabaseName=$($source.sourceName)")
    $propertiesLines.Add("mapping.$($source.index).targetSchemaName=$($source.targetSchema)")
}
$propertiesLines.Add("")
$propertiesLines.Add("compare.options.compareNullable=true")
$propertiesLines.Add("compare.options.compareDefaultValue=true")
$propertiesLines.Add("compare.options.compareLength=true")
$propertiesLines.Add("compare.options.sourceLoadThreads=4")
$propertiesLines.Add("")
$propertiesLines.Add("output.csvPath=build/reports/demo-large-report.csv")
$propertiesLines.Add("output.excelPath=build/reports/demo-large-detail.xlsx")
$propertiesLines.Add("output.summaryExcelPath=build/reports/demo-large-summary.xlsx")
$propertiesLines.Add("output.sqlPath=build/reports/demo-large-detail.sql")
$propertiesLines.Add("output.sqlTableName=db_compare_detail_result")
$propertiesLines.Add("output.summaryPath=build/reports/demo-large-summary.txt")

$propertiesPath = Join-Path $outputPath "demo-large.properties"
[System.IO.File]::WriteAllLines($propertiesPath, $propertiesLines, [System.Text.UTF8Encoding]::new($false))

Write-Host "Generated large demo dataset in $outputPath"
Write-Host ("Sources={0}, targetSchemas={1}, totalTables={2}" -f $SourceCount, $SourceCount, ($SourceCount * $TablesPerSource))

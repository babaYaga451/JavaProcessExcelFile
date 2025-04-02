package com.example.ProcessExcelFile.Data;

import com.poiji.annotation.ExcelCellName;
import com.poiji.annotation.ExcelSheet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ExcelSheet("Shipping Data")
public class Data {
    @ExcelCellName("Origin Zip")
    private Long origin;

    @ExcelCellName("Dest Zip")
    private Long destination;

    @ExcelCellName("GND TNT DAYS")
    private Long gndTntDays;

    @ExcelCellName("GND Zone")
    private Long gndZone;

    @ExcelCellName("Surepost TNT Days")
    private Long surePostTntDays;

    @ExcelCellName("Surepost Zone")
    private Long surePostZone;
}

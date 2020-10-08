package controllers
import play.api._
import play.api.Play.current
import controllers._
import models._
import org.apache.poi.openxml4j.opc._
import org.apache.poi.xssf.usermodel._
import com.github.nscala_time.time.Imports._
import java.io._
import java.nio.file.Files
import java.nio.file._
import org.apache.poi.ss.usermodel._

object ExcelUtility {
  val docRoot = "/report_template/"

  private def prepareTemplate(templateFile: String) = {
    val templatePath = Paths.get(current.path.getAbsolutePath + docRoot + templateFile)
    val reportFilePath = Files.createTempFile("temp", ".xlsx");

    Files.copy(templatePath, reportFilePath, StandardCopyOption.REPLACE_EXISTING)

    //Open Excel
    val pkg = OPCPackage.open(new FileInputStream(reportFilePath.toAbsolutePath().toString()))
    val wb = new XSSFWorkbook(pkg);

    (reportFilePath, pkg, wb)
  }

  def finishExcel(reportFilePath: Path, pkg: OPCPackage, wb: XSSFWorkbook) = {
    val out = new FileOutputStream(reportFilePath.toAbsolutePath().toString());
    wb.write(out);
    out.close();
    pkg.close();

    new File(reportFilePath.toAbsolutePath().toString())
  }

  def createStyle(mt: MonitorType.Value)(implicit wb: XSSFWorkbook) = {
    val prec = MonitorType.map(mt).prec
    val format_str = "0." + "0" * prec
    val style = wb.createCellStyle();
    val format = wb.createDataFormat();
    // Create a new font and alter it.
    val font = wb.createFont();
    font.setFontHeightInPoints(10);
    font.setFontName("標楷體");

    style.setFont(font)
    style.setDataFormat(format.getFormat(format_str))
    style.setBorderBottom(BorderStyle.THIN);
    style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
    style.setBorderLeft(BorderStyle.THIN);
    style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
    style.setBorderRight(BorderStyle.THIN);
    style.setRightBorderColor(IndexedColors.BLACK.getIndex());
    style.setBorderTop(BorderStyle.THIN);
    style.setTopBorderColor(IndexedColors.BLACK.getIndex());
    style
  }

  def createColorStyle(fgColors: Array[XSSFColor], mt: MonitorType.Value)(implicit wb: XSSFWorkbook) = {
    fgColors.map {
      color =>
        val style = createStyle(mt)
        style.setFillForegroundColor(color)
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND)
        style
    }
  }

  def getStyle(tag: String, normalStyle: XSSFCellStyle, abnormalStyles: Array[XSSFCellStyle]) = {
    import MonitorStatus._
    val info = MonitorStatus.getTagInfo(tag)
    info.statusType match {
      case StatusType.Internal =>
        {
          if (isValid(tag))
            normalStyle
          else if (isCalbration(tag))
            abnormalStyles(0)
          else if (isMaintenance(tag))
            abnormalStyles(1)
          else
            abnormalStyles(2)
        }
      case StatusType.Auto =>
        abnormalStyles(3)
      case StatusType.Manual =>
        abnormalStyles(4)
    }
  }

  import controllers.Highchart._
  def exportChartData(chart: HighchartData, monitorTypes: Array[MonitorType.Value]): File = {
    val precArray = monitorTypes.map { mt => MonitorType.map(mt).prec }
    exportChartData(chart, precArray)
  }

  def exportChartData(chart: HighchartData, precArray: Array[Int]) = {
    val (reportFilePath, pkg, wb) = prepareTemplate("chart_export.xlsx")
    val evaluator = wb.getCreationHelper().createFormulaEvaluator()
    val format = wb.createDataFormat();

    val sheet = wb.getSheetAt(0)
    val headerRow = sheet.createRow(0)
    headerRow.createCell(0).setCellValue("時間")

    var pos = 0
    for {
      col <- 1 to chart.series.length
      series = chart.series(col - 1)
    } {
      headerRow.createCell(pos + 1).setCellValue(series.name)
      pos += 1
    }

    val styles = precArray.map { prec =>
      val format_str = "0." + "0" * prec
      val style = wb.createCellStyle();
      style.setDataFormat(format.getFormat(format_str))
      style
    }

    // Categories data
    if (chart.xAxis.categories.isDefined) {
      val timeList = chart.xAxis.categories.get
      for (row <- timeList.zipWithIndex) {
        val rowNo = row._2 + 1
        val thisRow = sheet.createRow(rowNo)
        thisRow.createCell(0).setCellValue(row._1)

        for {
          col <- 1 to chart.series.length
          series = chart.series(col - 1)
        } {
          val cell = thisRow.createCell(col)
          cell.setCellStyle(styles(col - 1))

          val pair = series.data(rowNo - 1)
          if (pair.length == 2 && pair(1).isDefined) {
            cell.setCellValue(pair(1).get)
          }
          //val pOpt = series.data(rowNo-1)
          //if(pOpt.isDefined){
          //  cell.setCellValue(pOpt.get)
          //}

        }
      }
    } else {
      val rowMax = chart.series.map(s => s.data.length).max
      for (row <- 1 to rowMax) {
        val thisRow = sheet.createRow(row)
        val timeCell = thisRow.createCell(0)
        pos = 0
        for {
          col <- 1 to chart.series.length
          series = chart.series(col - 1)
        } {
          val cell = thisRow.createCell(pos + 1)
          pos += 1
          cell.setCellStyle(styles(col - 1))

          val pair = series.data(row - 1)
          if (col == 1) {
            val dt = new DateTime(pair(0).get.toLong)
            timeCell.setCellValue(dt.toString("YYYY/MM/dd HH:mm"))
          }
          if (pair(1).isDefined) {
            cell.setCellValue(pair(1).get)
          }
        }
      }
    }

    finishExcel(reportFilePath, pkg, wb)
  }

  def createDailyReport(monitor: Monitor.Value, reportDate: DateTime) = {
    implicit val (reportFilePath, pkg, wb) = prepareTemplate("dailyReport.xlsx")
    val format = wb.createDataFormat();
    val sheet = wb.getSheetAt(0)
    val titleRow = sheet.createRow(0)
    val dateRow = sheet.createRow(1)
    val legendRow = sheet.getRow(2)

    val fgColors =
      {
        val seqColors =
          for (col <- 3 to 7)
            yield legendRow.getCell(col).getCellStyle.getFillForegroundXSSFColor
        seqColors.toArray
      }

    val periodMap = Record.getRecordMap(Record.HourCollection)(MonitorType.mtvList, monitor, reportDate, reportDate + 1.day)
    val mtTimeMap = periodMap.map { pair =>
      val k = pair._1
      val v = pair._2
      k -> Map(v.map { r => r.time -> r }: _*)
    }
    val statMap = Query.getPeriodStatReportMap(periodMap, 1.day)(reportDate, reportDate + 1.day)

    def fillMonitorDailyReport(monitor: Monitor.Value) = {
      titleRow.createCell(0).setCellValue((Monitor.map(monitor).gcName + Monitor.map(monitor).selector) + "監測日報表")
      dateRow.createCell(0).setCellValue(s"日期:${reportDate.toString("yyyy年MM月dd日")}")

      for {
        mt_idx <- MonitorType.activeMtvList.zipWithIndex
        mt = mt_idx._1
        idx = mt_idx._2
        col = idx + 1
      } {
        sheet.getRow(3).createCell(col).setCellValue(MonitorType.map(mt).desp)
      }
      val normalStyleList = MonitorType.activeMtvList map createStyle
      val abnormalStyleList = MonitorType.activeMtvList map { createColorStyle(fgColors, _) }

      for {
        hour <- 0 to 23
        rowN = hour + 4
        row = sheet.createRow(rowN)
        timeLabel = row.createCell(0).setCellValue(s"$hour:00")

        mt_idx <- MonitorType.activeMtvList.zipWithIndex
        mt = mt_idx._1
        idx = mt_idx._2
        colN = idx + 1
        normalStyle = normalStyleList(idx)
        abnormalStyles = abnormalStyleList(idx)
        cell = row.createCell(colN)
        recordOpt = mtTimeMap(mt).get(reportDate + hour.hour)
      } {
        if (recordOpt.isEmpty) {
          cell.setCellValue("-")
        } else {
          val record = recordOpt.get
          val value = record.value
          val status = record.status
          cell.setCellValue(value)

          val cellStyle = getStyle(status, normalStyle, abnormalStyles)
          cell.setCellStyle(cellStyle)
        }
      }

      val avgRow = sheet.createRow(28)
      avgRow.createCell(0).setCellValue("平均")
      val maxRow = sheet.createRow(29)
      maxRow.createCell(0).setCellValue("最大")
      val minRow = sheet.createRow(30)
      minRow.createCell(0).setCellValue("最小")
      val effectRow = sheet.createRow(31)
      effectRow.createCell(0).setCellValue("有效率")

      for {
        mt_idx <- MonitorType.activeMtvList.zipWithIndex
        mt = mt_idx._1
        idx = mt_idx._2
        colN = idx + 1
      } {
        avgRow.createCell(colN).setCellValue(MonitorType.format(mt, statMap(mt)(reportDate).avg))
        maxRow.createCell(colN).setCellValue(MonitorType.format(mt, statMap(mt)(reportDate).max))
        minRow.createCell(colN).setCellValue(MonitorType.format(mt, statMap(mt)(reportDate).min))
        effectRow.createCell(colN).setCellValue(MonitorType.format(mt, statMap(mt)(reportDate).effectPercent))
      }

    }

    fillMonitorDailyReport(monitor)

    wb.setActiveSheet(0)
    finishExcel(reportFilePath, pkg, wb)
  }

  def createHistoryData(recordList: Seq[Record.RecordList], monitorTypes: Seq[MonitorType.Value]) = {
    implicit val (reportFilePath, pkg, wb) = prepareTemplate("historyData.xlsx")
    val format = wb.createDataFormat();
    val sheet = wb.getSheetAt(0)

    //Create header

    var row = 0
    val header = sheet.createRow(0)
    header.createCell(0).setCellValue("日期")
    header.createCell(1).setCellValue("選擇器")
    for ((mt, col) <- monitorTypes.zip(2 to 1 + monitorTypes.length)) {
      header.createCell(col).setCellValue(MonitorType.map(mt).desp)
    }
    val dateStyle = wb.createCellStyle()
    dateStyle.setDataFormat(format.getFormat("yyyy-mm-dd hh:mm"))
    
    for ((r, rowNum) <- recordList.zip(1 to recordList.size)) {
      val row = sheet.createRow(rowNum)
      val dateCell = row.createCell(0)
      dateCell.setCellStyle(dateStyle)
      dateCell.setCellValue(new DateTime(r.time).toDate())
      row.createCell(1).setCellValue(r.monitor)
      for ((mt, colNum) <- monitorTypes.zip(2 to 1 + monitorTypes.length)) {
        val mtDataOpt = r.mtDataList.find(mtdt => mtdt.mtName == mt.toString())
        val cell = row.createCell(colNum)
        if (mtDataOpt.isDefined) {
          cell.setCellValue(mtDataOpt.get.value)
        } else {
          cell.setCellValue("-")
        }
      }
    }

    wb.setActiveSheet(0)
    finishExcel(reportFilePath, pkg, wb)
  }
}
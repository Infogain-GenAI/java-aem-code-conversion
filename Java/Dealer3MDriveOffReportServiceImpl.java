package com.haea.daehyundai.core.reports.services.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.NumberUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.haea.daehyundai.core.pojos.ApiResponse;
import com.haea.daehyundai.core.reports.services.DCAReportAPIGatewayService;
import com.haea.daehyundai.core.reports.services.Dealer3MDriveOffReportService;
import com.haea.daehyundai.core.reports.services.HyundaiReportsDCAAPIPathConfigService;
import com.haea.daehyundai.core.reports.services.HyundaiReportsDCAConfigService;
import com.haea.daehyundai.core.reports.services.HyundaiReportsDCAIFIDConfigService;
import com.haea.daehyundai.core.reports.util.CommonReportDCAConstants;

@Component(service = Dealer3MDriveOffReportService.class, immediate = true)
public class Dealer3MDriveOffReportServiceImpl implements Dealer3MDriveOffReportService {

	private final Logger log = LoggerFactory.getLogger(Dealer3MDriveOffReportServiceImpl.class);

	@Reference
	private transient DCAReportAPIGatewayService apiGatewayService;

	@Reference
	private HyundaiReportsDCAConfigService hyundaiDcaConfigService;

	@Reference
	private HyundaiReportsDCAIFIDConfigService hyundaiIfConfigService;

	@Reference
	private HyundaiReportsDCAAPIPathConfigService hyundaiAPIPathConfigService;

	@Override
	public XSSFWorkbook exportExcel3MDOR(SlingHttpServletRequest request) {
		Map<String, String> requestHeaders = getRequestParameter(request,this.hyundaiIfConfigService.getReport3MDOIFID());
		ApiResponse response = getDealerMtdReport(requestHeaders, request,this.hyundaiAPIPathConfigService.getReport3MDORURL());
		XSSFWorkbook xssfWorkbook = generateExcelWithValues(response);
		return xssfWorkbook;
	}

	private ApiResponse getDealerMtdReport(Map<String, String> requestHeaders, SlingHttpServletRequest request, String url) {
		String appname = "";
		ApiResponse response = new ApiResponse();
		response = apiGatewayService.getDCAReportAPIResponse(url,
				StringUtils.EMPTY, requestHeaders, appname, request);
		return response;
	}

	private Map<String, String> getRequestParameter(SlingHttpServletRequest request, String ifid) {
		Map<String, String> requestHeaders = new HashMap<>();
		requestHeaders.put(CommonReportDCAConstants.REQUEST_TYPE, "GET");
		requestHeaders.put(CommonReportDCAConstants.IFID, ifid);
		requestHeaders.put(CommonReportDCAConstants.BRANDID, this.hyundaiDcaConfigService.getBrandId());
		requestHeaders.put(CommonReportDCAConstants.COMPANY, this.hyundaiDcaConfigService.getCompany());
		requestHeaders.put(CommonReportDCAConstants.SENDER, this.hyundaiDcaConfigService.getSender());
		requestHeaders.put(CommonReportDCAConstants.RECEIVER, this.hyundaiDcaConfigService.getReceiver());
		requestHeaders.put(CommonReportDCAConstants.DEALERHMA,
				request.getParameter(CommonReportDCAConstants.DEALERHMA));
		requestHeaders.put(CommonReportDCAConstants.DEALERCODE,
				request.getParameter(CommonReportDCAConstants.DEALERCODE));
		return requestHeaders;
	}

	private XSSFWorkbook generateExcelWithValues(ApiResponse response) {
		XSSFWorkbook workbook = null;

		String dealerResponse = response.getResponse();

		workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet("VIN Detail");

		CellStyle style = workbook.createCellStyle();
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);

		XSSFCellStyle headerStyle = workbook.createCellStyle();
		headerStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(31, 73, 125)));
		headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		XSSFFont font = workbook.createFont();
		font.setColor(IndexedColors.WHITE.getIndex());
		font.setBold(true);
		headerStyle.setFont(font);
		headerStyle.setAlignment(HorizontalAlignment.CENTER);
		headerStyle.setBorderBottom(BorderStyle.THIN);
		headerStyle.setBorderTop(BorderStyle.THIN);
		headerStyle.setBorderLeft(BorderStyle.THIN);
		headerStyle.setBorderRight(BorderStyle.THIN);

		printExcelSheetRows(sheet, dealerResponse, style, headerStyle, CommonReportDCAConstants.DRIVEOFF_HEADERS);

		return workbook;
	}

	private void printExcelSheetRows(XSSFSheet sheet, String dealerReport, CellStyle style, XSSFCellStyle headerStyle, String[] headerNames) {
		try {
			
			XSSFRow rowhead = sheet.createRow((short) 0);
			int counter = 0;
			for (String headerName : headerNames ) {
				rowhead.createCell(counter).setCellValue(headerName);
				rowhead.getCell(counter).setCellStyle(headerStyle);
				counter++;
			}
			resizeColumns(sheet, counter);
			
			JSONObject dealerObject = new JSONObject(dealerReport);
			JSONArray dealerArray = new JSONArray(dealerObject.get("RESPONSE_STRING").toString());
			
			for (int i = 0; i < dealerArray.length(); i++) {
				JSONObject value = dealerArray.getJSONObject(i);
				int row_length = value.length();
				Iterator<String> headers = value.keys();
				XSSFRow row = sheet.createRow(i + 1);
				for (int j = 0; j < row_length; j++) {
					String head = headers.next();
					if (value.get(head).equals(null))
						row.createCell(j).setCellValue("NA");
					else if (NumberUtils.isNumber(value.getString(head)))
						row.createCell(j).setCellValue(value.getInt(head));
					else
						row.createCell(j).setCellValue(value.getString(head));
					row.getCell(j).setCellStyle(style);
				}
			}
			int numberOfColumns = dealerArray.getJSONObject(0).length();
			resizeColumns(sheet, numberOfColumns);
		} catch (JSONException e) {
			log.error("JSONException {}", e.getMessage());
		}
	}
	
	private void resizeColumns(XSSFSheet sheet, int numberOfColumns) {
		int[] maxLengthStrings = new int[numberOfColumns];
		for (Row row : sheet) {
			for (int i = 0; i < numberOfColumns; i++) {
				Cell cell = row.getCell(i);
				CellType cellType = cell.getCellTypeEnum();
				String cellValue = "";
				if (cellType.equals(CellType.STRING)) {
					cellValue = cell.getStringCellValue();
				} else {
					cellValue = String.valueOf(cell.getNumericCellValue());
				}
				int cellValueLength = cellValue.length();
				if (maxLengthStrings[i] < cellValueLength) {
					maxLengthStrings[i] = cellValueLength;
				}
			}
		}
		for (int i = 0; i < numberOfColumns; i++) {
			int columnWidth = (int) ((maxLengthStrings[i] * 1.14388) + 2) * 256;
			sheet.setColumnWidth(i, columnWidth);
		}
	}
}

/*global define */
define(['excel-builder'], function(excelBuilder) {
	'use strict';

  function createXlsxFile(restApiHttp400ResponseData) {
    if (typeof restApiHttp400ResponseData === 'string' || restApiHttp400ResponseData instanceof String) {
      restApiHttp400ResponseData = JSON.parse(restApiHttp400ResponseData);
    }
    var errors = restApiHttp400ResponseData && restApiHttp400ResponseData.errors ? restApiHttp400ResponseData.errors : [];
    var workbook = excelBuilder.createWorkbook();
    var worksheet = workbook.createWorksheet({name: 'Λάθη Εισαγωγής'});
    var stylesheet = workbook.getStyleSheet();
    var outputData = [];
    var headerFormatter = stylesheet.createFormat({
      font: {
        bold: true
      }
    });
    outputData.push([{value:'Γραμμή', metadata: {style: headerFormatter.id}}, {value: 'Στήλη Από-Έως', metadata: {style: headerFormatter.id}},
      {value: 'Απορριφθείσα Τιμή', metadata: {style: headerFormatter.id}}, {value: 'Κωδικός Σφάλματος', metadata: {style: headerFormatter.id}},
      {value: 'Μήνυμα Σφάλματος', metadata: {style: headerFormatter.id}}]);
    for (var i=0; i < errors.length; i++) {
      var line = [];
      if (errors[i].property) {
        line.push(errors[i].property.split(':')[0] ? errors[i].property.split(':')[0] : '');
        line.push(errors[i].property.indexOf(':') > -1 ? errors[i].property.split(':')[1] : '');
        line.push(errors[i].invalidValue);
        line.push(errors[i].code);
        line.push(errors[i].message);
        outputData.push(line);
      } else {
        line.push(errors[i].message);
        outputData.push(line);
      }
    }
    worksheet.setData(outputData);
    worksheet.setColumns([
      {width: 10},
      {width: 20},
      {width: 20},
      {width: 40},
      {width: 80}
    ]);
    workbook.addWorksheet(worksheet);
    return excelBuilder.createFile(workbook, {base64: true, type:'blob'});
  }

  return createXlsxFile;
});
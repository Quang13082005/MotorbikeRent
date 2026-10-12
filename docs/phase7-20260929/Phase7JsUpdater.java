import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class Phase7JsUpdater {
    public static void main(String[] args) throws Exception {
        updateStaffJs();
        System.out.println("staff.js updated successfully without mojibake.");
    }

    private static void updateStaffJs() throws Exception {
        Path path = Path.of("src/main/resources/static/js/staff.js");
        String content = Files.readString(path, StandardCharsets.UTF_8);

        content = content.replace("\r\n", "\n");

        String staffModalDef = 
                "  async function completeStaffModal(id){\n" +
                "    const order=await api('/api/rental-orders/'+id);\n" +
                "    let held=0;\n" +
                "    (order.payments||[]).forEach(p=>{\n" +
                "      if(p.status==='PAID'||p.status==='REFUNDED'){\n" +
                "        const t=p.transactionType||p.purpose;\n" +
                "        if(['ASSET_DEPOSIT','ASSET_DEPOSIT_ADDITIONAL','SECURITY'].includes(t))held+=Math.abs(p.amount);\n" +
                "        else if(['ASSET_DEPOSIT_REFUND','DEPOSIT_DEDUCTION','SECURITY_REFUND'].includes(t))held-=Math.abs(p.amount);\n" +
                "      }\n" +
                "    });\n" +
                "    if(held<0)held=0;\n" +
                "    const isOverdue=new Date()>new Date(order.returnDate);\n" +
                "    const isReturnReq=order.status==='RETURN_REQUESTED';\n" +
                "    const panel=document.createElement('dialog');\n" +
                "    panel.style.cssText='max-width:560px;width:95%;padding:24px;border-radius:18px;background:#102331;color:#f8fafc;border:1px solid #334155;box-shadow:0 25px 50px -12px rgba(0,0,0,0.5)';\n" +
                "    panel.innerHTML='<h2 style=\"font-size:20px;font-weight:700;margin-bottom:6px\">Hoàn tất đơn thuê xe</h2>'+\n" +
                "    '<p class=\"cw-muted\" style=\"margin-bottom:12px;font-size:14px\">'+(order.orderCode||'')+' · Khách: '+(order.customerName||'')+'</p>'+\n" +
                "    (isOverdue?'<div style=\"color:#ef4444;background:#ef444422;padding:8px 12px;border-radius:8px;margin-bottom:12px;font-size:13px;font-weight:600\">⚠️ Trả xe quá hạn! Hạn trả: '+(order.returnDate||'')+'</div>':'')+\n" +
                "    (isReturnReq?'<div style=\"color:#f59e0b;background:#f59e0b22;padding:8px 12px;border-radius:8px;margin-bottom:12px;font-size:13px\">ℹ️ Khách có yêu cầu trả xe sớm</div>':'')+\n" +
                "    '<p class=\"cw-message\" role=\"status\" style=\"margin-bottom:8px\"></p>'+\n" +
                "    '<form class=\"cw-grid\">'+\n" +
                "    '<div class=\"cw-full\" style=\"background:#1e293b;padding:12px;border-radius:10px;margin-bottom:6px\">'+\n" +
                "    '<div style=\"font-size:12px;color:#94a3b8\">Tiền cọc tài sản đang giữ</div>'+\n" +
                "    '<strong style=\"font-size:18px;color:#38bdf8\">'+held.toLocaleString('vi-VN')+' đ</strong>'+\n" +
                "    '</div>'+\n" +
                "    '<label class=\"cw-field\">Số tiền hoàn cọc (VNĐ)<input type=\"number\" min=\"0\" max=\"'+held+'\" step=\"1000\" name=\"assetDepositRefundAmount\" value=\"'+held+'\" required></label>'+\n" +
                "    '<label class=\"cw-field\">Khấu trừ cọc (VNĐ)<input type=\"number\" min=\"0\" max=\"'+held+'\" step=\"1000\" name=\"assetDepositDeductionAmount\" value=\"0\" required></label>'+\n" +
                "    '<label class=\"cw-field cw-full\">Lý do khấu trừ cọc (nếu có)<input type=\"text\" name=\"depositDeductionReason\" placeholder=\"Ví dụ: bồi thường trầy xước, thiếu phụ kiện...\"></label>'+\n" +
                "    '<label class=\"cw-field\">Phương thức hoàn cọc<select name=\"depositRefundMethod\"><option value=\"CASH\">Tiền mặt</option><option value=\"BANK_TRANSFER\">Chuyển khoản</option><option value=\"OTHER\">Khác</option></select></label>'+\n" +
                "    '<label class=\"cw-field\">Phí trả trễ (VNĐ)<input type=\"number\" min=\"0\" step=\"1000\" name=\"lateFee\" value=\"0\"></label>'+\n" +
                "    '<label class=\"cw-field cw-full\">Lý do thu phí trễ<input type=\"text\" name=\"lateFeeReason\" placeholder=\"Ví dụ: Khách trả trễ 3 giờ\"></label>'+\n" +
                "    '<label class=\"cw-field\">Phương thức thu phí trễ<select name=\"lateFeePaymentMethod\"><option value=\"CASH\">Tiền mặt</option><option value=\"BANK_TRANSFER\">Chuyển khoản</option><option value=\"OTHER\">Khác</option></select></label>'+\n" +
                "    (isReturnReq?'<div class=\"cw-full\" style=\"border-top:1px solid #334155;padding-top:10px;margin-top:6px\">'+\n" +
                "    '<div style=\"font-weight:600;font-size:13px;margin-bottom:6px\">Quyết toán trả xe sớm:</div>'+\n" +
                "    '<label class=\"cw-field\">Hoàn tiền thuê trả sớm (VNĐ)<input type=\"number\" min=\"0\" step=\"1000\" name=\"refundAmount\" value=\"0\"></label>'+\n" +
                "    '<label class=\"cw-field\">Lý do hoàn tiền thuê<input type=\"text\" name=\"refundReason\" value=\"Khách trả xe sớm\"></label>'+\n" +
                "    '<label class=\"cw-field\">Hình thức hoàn tiền thuê<select name=\"paymentMethod\"><option value=\"CASH\">Tiền mặt</option><option value=\"BANK_TRANSFER\">Chuyển khoản</option><option value=\"OTHER\">Khác</option></select></label>'+\n" +
                "    '</div>':'')+\n" +
                "    '<button class=\"cw-btn primary\" style=\"margin-top:14px;width:100%\">Xác nhận hoàn tất đơn</button>'+\n" +
                "    '</form>'+\n" +
                "    '<button class=\"cw-btn\" data-close style=\"margin-top:10px;width:100%\">Đóng</button>';\n" +
                "    document.body.append(panel);\n" +
                "    panel.showModal();\n" +
                "    panel.querySelector('[data-close]').onclick=()=>{panel.close();panel.remove();};\n" +
                "    const form=panel.querySelector('form');\n" +
                "    const refIn=form.elements.namedItem('assetDepositRefundAmount');\n" +
                "    const dedIn=form.elements.namedItem('assetDepositDeductionAmount');\n" +
                "    if(refIn&&dedIn&&held>0){\n" +
                "      refIn.oninput=()=>{const v=Math.min(held,Math.max(0,Number(refIn.value)||0));dedIn.value=held-v;};\n" +
                "      dedIn.oninput=()=>{const v=Math.min(held,Math.max(0,Number(dedIn.value)||0));refIn.value=held-v;};\n" +
                "    }\n" +
                "    form.onsubmit=async e=>{\n" +
                "      e.preventDefault();\n" +
                "      const btn=form.querySelector('button.primary');btn.disabled=true;\n" +
                "      try{\n" +
                "        const fd=new FormData(form);\n" +
                "        const payload={\n" +
                "          assetDepositRefundAmount:Number(fd.get('assetDepositRefundAmount')||0),\n" +
                "          assetDepositDeductionAmount:Number(fd.get('assetDepositDeductionAmount')||0),\n" +
                "          depositDeductionReason:fd.get('depositDeductionReason')?.trim()||null,\n" +
                "          depositRefundMethod:fd.get('depositRefundMethod')||'CASH',\n" +
                "          lateFee:Number(fd.get('lateFee')||0),\n" +
                "          lateFeeReason:fd.get('lateFeeReason')?.trim()||null,\n" +
                "          lateFeePaymentMethod:fd.get('lateFeePaymentMethod')||'CASH',\n" +
                "          refundAmount:Number(fd.get('refundAmount')||0),\n" +
                "          refundReason:fd.get('refundReason')?.trim()||null,\n" +
                "          paymentMethod:fd.get('paymentMethod')||'CASH'\n" +
                "        };\n" +
                "        await api('/api/rental-orders/'+id+'/complete',{\n" +
                "          method:'POST',\n" +
                "          headers:{'Content-Type':'application/json'},\n" +
                "          body:JSON.stringify(payload)\n" +
                "        });\n" +
                "        panel.close();panel.remove();\n" +
                "        location.reload();\n" +
                "      }catch(err){panel.querySelector('.cw-message').textContent=err.message;btn.disabled=false;}\n" +
                "    };\n" +
                "  }\n";

        String targetEnd = "})();";
        content = content.replace(targetEnd, staffModalDef + "\n" + targetEnd);
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}

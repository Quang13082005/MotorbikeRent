from pathlib import Path
import re
r=Path('src/main/resources')
p=r/'static/js/booking-context.js';s=p.read_text(encoding='utf-8');s=re.sub(r' const get=\(\)=>.*?;\n', ' const get=()=>read();\n',s);p.write_text(s,encoding='utf-8')
p=r/'templates/customer/rental-contract.html';s=p.read_text(encoding='utf-8').replace('const urlParams = new URLSearchParams(window.location.search);','const urlParams = new URLSearchParams(window.BookingContext.get());');s=s.replace("            const amount = window.currentDepositAmount || 0;", "            if(!orderId)throw new Error('Máy chủ chưa trả về mã đơn thuê.');")
s=s.replace("paymentData.error || 'Lỗi không xác định từ PayOS.'","paymentData.message || paymentData.error || 'Chưa kết nối được PayOS. Bạn có thể thanh toán lại trong Đơn thuê của tôi.'")
a=s.index('        // Ép cứng bằng JS');b=s.index('        // Luôn cập nhật min',a);s=s[:a]+'''        const problem=BookingContext.valid(pickupInput.value,returnInput.value,true);
        pickupInput.min=BookingContext.now();
        if(problem){quoteReady=false;document.getElementById('submitBookingBtn').disabled=true;const error=document.getElementById('timeErrorMsg');error.textContent=problem;error.classList.remove('hidden');return;}
        BookingContext.save({pickup:pickupInput.value,returnAt:returnInput.value});
'''+s[b:]
s=s.replace("        const requestBody=bookingRequest();", "        const timeProblem=BookingContext.valid(pickupDate,returnDate,true);if(timeProblem){alert(timeProblem);return;}\n        const requestBody=bookingRequest();")
s=s.replace("const req=bookingRequest();if(!req.pickupDate || !req.returnDate)return;", "const req=bookingRequest();const problem=BookingContext.valid(req.pickupDate,req.returnDate,true);if(problem){const error=document.getElementById('timeErrorMsg');error.textContent=problem;error.classList.remove('hidden');return;}")
# Preserve independently chosen return address on reload; initial landing selection supplies both.
needle="        // Init Time Pickers"
s=s.replace(needle,'''        if(urlParams.has('returnLocation')){
            const type=urlParams.get('returnLocation');document.getElementById('returnLocationSelect').value=type;
            document.getElementById('returnAddressInput').value=type==='store'?'Trả tại cửa hàng':urlParams.get('returnAddress')||'';
            coordinates.return=type==='store'?null:{lat:Number(urlParams.get('returnLat')),lng:Number(urlParams.get('returnLng'))};
            if(type!=='store')document.getElementById('returnMapOption').textContent='Trả tại: '+urlParams.get('returnAddress');
        }
'''+needle)
a=s.index('            // Auto copy to return');b=s.index("        } else if (targetId === 'returnAddressInput')",a);s=s[:a]+s[b:]
s=s.replace('        coordinates[targetId === "pickupAddressInput" ? "pickup" : "return"]={lat,lng};','''        coordinates[targetId === "pickupAddressInput" ? "pickup" : "return"]={lat,lng};
        BookingContext.save(targetId==='pickupAddressInput'?{location:'maps_delivery',deliveryAddress:addr,deliveryLat:String(lat),deliveryLng:String(lng)}:{returnLocation:'maps_delivery',returnAddress:addr,returnLat:String(lat),returnLng:String(lng)});''')
s=s.replace("document.getElementById(type + 'AddressInput').value = type === 'pickup' ? 'Nhận tại cửa hàng' : 'Trả tại cửa hàng';", "document.getElementById(type + 'AddressInput').value = type === 'pickup' ? 'Nhận tại cửa hàng' : 'Trả tại cửa hàng';\n            coordinates[type]=null;BookingContext.save(type==='pickup'?{location:'store',deliveryAddress:'',deliveryLat:'',deliveryLng:''}:{returnLocation:'store',returnAddress:'',returnLat:'',returnLng:''});")
p.write_text(s,encoding='utf-8')
# Explicit map confirmation from landing/list sets the shared initial delivery/return point.
p=r/'templates/fragments/map-modal.html';s=p.read_text(encoding='utf-8').replace("save({location:'maps_delivery',deliveryAddress:addr,deliveryLat:String(lat),deliveryLng:String(lng)})", "save({location:'maps_delivery',deliveryAddress:addr,deliveryLat:String(lat),deliveryLng:String(lng),returnLocation:'maps_delivery',returnAddress:addr,returnLat:String(lat),returnLng:String(lng)})");p.write_text(s,encoding='utf-8')

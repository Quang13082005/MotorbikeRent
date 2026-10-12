from pathlib import Path
import re, shutil
root=Path('src/main'); j=root/'java/com/motorrent/motorbikerental'; r=root/'resources'
def edit(p,f):
 s=p.read_text(encoding='utf-8');p.write_text(f(s),encoding='utf-8')
for rel in ['entity/Station.java','repository/StationRepository.java','entity/Promotion.java','repository/PromotionRepository.java','mapper/PromotionMapper.java','dto/request/PromotionRequest.java','dto/response/PromotionResponse.java']:
 p=j/rel; dst=Path('docs/revision-20260927/removed')/rel;dst.parent.mkdir(parents=True,exist_ok=True);shutil.move(p,dst)
p=r/'templates/admin/promotions.html';shutil.move(p,'docs/revision-20260927/removed/promotions.html')
edit(j/'entity/Motorbike.java',lambda s: re.sub(r'    @ManyToOne\(fetch = FetchType.LAZY\)\s+@JoinColumn\(name = "station_id"\)\s+Station station;\s*','',s))
edit(j/'mapper/MotorbikeMapper.java',lambda s:s.replace('    @Mapping(source = "station.name", target = "station")\n',''))
for rel,line in [('dto/request/MotorbikeRequest.java','    Long stationId;\n'),('dto/response/MotorbikeDetailResponse.java','    String station;\n'),('dto/request/RentalOrderRequest.java','    String promotionCode;\n')]:edit(j/rel,lambda s:s.replace(line,''))
edit(j/'service/OperationsService.java',lambda s:re.sub(r'   case "promotions" -> \{\n.*?\n   }\n','',re.sub(r'^   case "promotions" -> .*\n','',s.replace(' private final StationRepository stations;','').replace('private final PromotionRepository promotions; ',''),flags=re.M),flags=re.S))
edit(j/'service/PricingService.java',lambda s:re.sub(r'  if\(r.getPromotionCode\(\)!=null.*?\n  }\n','',s.replace(' private final PromotionRepository promotions;\n',''),flags=re.S))
edit(j/'service/RentalOrderService.java',lambda s: '\n'.join(l for l in s.split('\n') if not any(x in l for x in ['private final com.motorrent.motorbikerental.repository.PromotionRepository','entity.Promotion promotion=','if(request.getPromotionCode()','if(promotion!=null)'])).replace('.promotionCode(promotion==null?null:promotion.getCode()).discountAmount((Integer)q.get("discountAmount"))','.discountAmount(0)'))
edit(j/'controller/web/AdminPageController.java',lambda s:s.replace('"promotions",',''))
edit(r/'templates/fragments/admin-layout.html',lambda s:re.sub(r'<a href="/admin/promotions".*?</a>','',s))
edit(r/'static/js/operations-live.js',lambda s:re.sub(r'^  promotions:.*\n','',s,flags=re.M))
edit(r/'templates/customer/rental-contract.html',lambda s:re.sub(r'<label class="cw-field">Mã khuyến mãi.*?</label><p id="promotionSaving".*?</p>','',s).replace(',promotionCode:document.getElementById("promotionCode").value','').replace("            document.getElementById('promotionSaving').textContent=q.discountAmount ? 'Đã giảm '+money(q.discountAmount) : '';",''))
for p in Path('src/test/java').rglob('*.java'):edit(p,lambda s:s.replace('"promotions",','').replace('"admin/promotions", ',''))
# Historical order amounts remain immutable audit snapshots, without any promotion functionality.
edit(j/'entity/RentalOrder.java',lambda s:s.replace('    String promotionCode;','    // Historical snapshot only; promotions are no longer offered.\n    String promotionCode;'))
edit(r/'templates/customer/home.html',lambda s:re.sub(r'<section class="customer-workspace" id="ai-assistant">.*?</section>','',s))

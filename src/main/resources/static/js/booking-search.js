(() => {
 const form=document.getElementById('home-search')||document.getElementById('catalog-search');if(!form)return;
 const start=form.elements.pickup,end=form.elements.returnAt,place=form.elements.location,ctx=BookingContext;
 const state=ctx.get();
 for(const k of ['pickup','returnAt'])if(state[k]&&!form.elements[k].value)form.elements[k].value=state[k];
 if(state.location)place.value=state.location;
 for(const [key,id] of [['deliveryAddress','prefilledAddress'],['deliveryLat','prefilledLat'],['deliveryLng','prefilledLng']]){
  let input=form.elements[key];if(!input){input=document.createElement('input');input.type='hidden';input.name=key;input.id=id;form.append(input);}if(state[key])input.value=state[key];
 }
 const refresh=()=>{start.min=ctx.now();end.min=start.value||start.min;};refresh();
 for(const input of [start,end]){input.addEventListener('focus',refresh);input.addEventListener('input',()=>{start.setCustomValidity('');end.setCustomValidity('');refresh();});}
 const label=()=>{const opt=place.querySelector('option[value="maps_delivery"]');if(opt&&form.elements.deliveryAddress.value)opt.textContent='Giao tại: '+form.elements.deliveryAddress.value;};label();
 place.addEventListener('change',()=>{if(place.value==='store'){ctx.save({location:'store',deliveryAddress:'',deliveryLat:'',deliveryLng:''});for(const k of ['deliveryAddress','deliveryLat','deliveryLng'])form.elements[k].value='';} });
 form.addEventListener('submit',event=>{
  refresh();start.setCustomValidity('');end.setCustomValidity('');
  const problem=ctx.valid(start.value,end.value,form.id==='home-search');
  if(problem){event.preventDefault();start.setCustomValidity(problem);start.reportValidity();return;}
  if(place.value!=='store'&&(!form.elements.deliveryAddress.value||!form.elements.deliveryLat.value||!form.elements.deliveryLng.value)){
   event.preventDefault();place.setCustomValidity('Bấm địa điểm nhận xe để chọn vị trí giao trước khi tìm kiếm.');place.reportValidity();return;
  }
  place.setCustomValidity('');
  if(form.id==='catalog-search'){
   form.querySelectorAll('input[name="category"]').forEach(el=>el.remove());
   if(event.submitter?.name!=='category'){const input=document.createElement('input');input.type='hidden';input.name='category';input.value=document.getElementById('current-category').value;form.append(input);}
  }
  ctx.save(Object.fromEntries(new FormData(form)));
 });
 place.addEventListener('focus',()=>place.setCustomValidity(''));
})();

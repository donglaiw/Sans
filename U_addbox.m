function im=U_addbox(im,rr,cc,ww)
num_r = numel(rr);
num_r2 = num_r+mod(num_r,2);
cs= uint8(255*colormap('jet'));
cs = cs(1:floor(size(cs,1)/num_r2):end,:);


mid = num_r2/2;
ind = reshape([1:mid;(1+mid):num_r2],1,[]);
cs = cs(ind,:);

for i=1:num_r 
    for k=1:3
    im(rr{i}(1)-ww:rr{i}(1)+ww,cc{i}(1):cc{i}(2),k) = cs(i,k);
    im(rr{i}(2)-ww:rr{i}(2)+ww,cc{i}(1):cc{i}(2),k) = cs(i,k);
    im(rr{i}(1):rr{i}(2),cc{i}(1)-ww:cc{i}(1)+ww,k) = cs(i,k);
    im(rr{i}(1):rr{i}(2),cc{i}(2)-ww:cc{i}(2)+ww,k) = cs(i,k);
    end
end
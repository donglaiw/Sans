function feat=U_stat(im2_a,im2_b)
sz =size(im2_a);
[yy,xx]=meshgrid(1:sz(2),1:sz(1));
feat=zeros(4,im2_b);
for i=1:im2_b
    tmp_x = xx(im2_a==i);
    tmp_y = yy(im2_a==i);
    feat(:,i) = [mean(tmp_x) min(tmp_x) range(tmp_x) range(tmp_y)]';
end


im = imread('test.jpg');

imagesc(im(:,:,1))
% adaptive select threshold

im2 = im(:,:,1)<130;
% erosion a bit
%{
se = strel('disk',2);
im3 = imerode(im2,se);
%}
im3=im2;
[im2_a,im2_b]=bwlabel(im3);
im2_c = histc(im2_a(:),1:im2_b);
%{
%}
% select obj I: sz
sz = size(im2);
% can be small: broken parts
% can be big: connected
%{
thres1 = [50 inf];
ind1 = find(im2_c>thres1(1) & im2_c<thres1(2));
%}
ind1 = find(im2_c>thres1(1) & im2_c<thres1(2));
%tmp=zeros(sz);tmp(ismember(im2_a,ind1))=1;imagesc(tmp)

% select obj II: shape
[yy,xx]=meshgrid(1:sz(2),1:sz(1));
num_seg = numel(ind1);
feat=zeros(4,num_seg);
% x,y,h,w
for i=1:num_seg
    tmp_x = xx(im2_a==ind1(i));
    tmp_y = yy(im2_a==ind1(i));
    feat(:,i) = [mean(tmp_x) mean(tmp_y) range(tmp_x) range(tmp_y)]';
end
char_h = median(feat(3,:));

thres2=[char_h/5 char_h*5];
%ind2 = ind1(min(feat(3:4,:))>thres2(1)&min(feat(3:4,:))<thres2(2));

% num row
im4 = im3.*ismember(im2_a,ind2);
rowsum = sum();
findpeak
% row clustering
cid=2;
switch cid
    case 1
        % a. kmeans
        center = min(feat(1,:)):char_h:max(feat(2,:));
        num_k = numel(center);
        [kid,kc] = kmeans(feat(1,:),num_k);
    case 2
        % b. hierarchial
        d = pdist(feat(1,:)');
        Z = linkage(d,'complete');
        kid = cluster(Z,'cutoff',char_h*3,'criterion','distance');
        %kid = cluster(Z,'maxclust',15);
end
%{
tmp=zeros(sz);
for kk=1:num_k;tmp(ismember(im2_a,ind1(kid==kk)))=kk;end;
imagesc(tmp)
%}
feat(1,:)

% row output

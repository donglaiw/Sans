im = imread('test.jpg');

% adaptive select threshold
im1 = im(:,:,1)<130;
sz = size(im1);
% safe margin
mm = 200;
im1(1:mm,:) = 0;im1(:,1:mm) = 0;im1(end-mm:end,:) = 0;im1(:,end-mm:end) = 0;

% noise/broken part
sz_n = 500;

% seg
[im2_a,im2_b]=bwlabel(im1);
im2_c = histc(im2_a(:),1:im2_b);
char_h = ceil(median(sqrt(im2_c(im2_c>sz_n))));
im2 = im1.*ismember(im2_a,find(im2_c>sz_n));
[im2_a,im2_b]=bwlabel(im2);

% row number
rowsum = sum(im2,2);
row_dif = ceil(char_h*1.5);
%colsum = sum(im2);
peak_thres = prctile(rowsum,80);
[row_p,row_id]=findpeaks(rowsum,'MINPEAKDISTANCE',row_dif,'MINPEAKHEIGHT',peak_thres);
%plot(rowsum),hold on,plot(row_id,row_p,'rx'),hold off

num_r = numel(row_id);

%{
% better remove junk with chunck
im2(1:min(row_id)-2*char_h,:)=0;
im2(max(row_id)+2*char_h:end,:)=0;
[im2_a,im2_b]=bwlabel(im2);
%}
% row assignment
% x,min_x,h,w
feat=U_stat(im2_a,im2_b);




% row clustering
cid=3;
switch cid
    case 1
        % a. kmeans
        [kid,kc] = kmeans(feat(1,:),num_r);
    case 2
        % b. hierarchial
        d = pdist(feat(1,:)','minkowski',1);
        Z = linkage(d,'average');
        kid = cluster(Z,'maxclust',num_r);
    case 3
        % known row_id
        dis = pdist2(feat(1,:)',row_id);
        [m_d,kid] = min(dis,[],2);
        kid(m_d>2*char_h) = -1;
end
%{
tmp=zeros(sz);
for kk=1:num_r;tmp(ismember(im2_a,find(kid==kk)))=kk;end;
imagesc(tmp)
%}

nn='mscoll390_item349_wk1_body0002_crop1.jpg';
% row output
margin_l = ceil(char_h*1.3);
rrs= cell(1,num_r);
ccs= cell(1,num_r);
cc_ran = ceil(char_h*2);
for kk=1:num_r
    % hori: two liner
    rrs{kk}= ceil([prctile(feat(1,kid==kk),20) prctile(feat(1,kid==kk),80)]+margin_l*[-1 1]);
    % verti: range    
    cc = conv2(double(im1(rrs{kk}(1):rrs{kk}(2),:)),ones(range(rrs{kk})+1,cc_ran),'valid');
    cid = find(cc(1:cc_ran:end)>char_h^2*0.3);
    if ~isempty(cid)
        ccs{kk} = [cid(1)-1 cid(end)+1]*cc_ran;       
    end
end
rrs= rrs(~cellfun(@isempty,ccs));
ccs= ccs(~cellfun(@isempty,ccs));
[~,rid]=sort(cellfun(@(x) x(1),rrs),'ascend');
% human check
imwrite(U_addbox(im,rrs,ccs,3),[nn(1:end-4) '_bbox.jpg']);

cc=1;
w1 = 1/3;
line_thres = char_h/2;
% dot/bar map 
im3 = im1; im3(im2_a>0)=0;
for i=rid    
    % center xs are out of range 
    ind1 = find(feat(1,:)>rrs{i}(1)-line_thres & feat(1,:)<line_thres+rrs{i}(2));    
    % min/max xs are out of range
    max_thres = (rrs{i}(2)-rrs{i}(1))*w1+rrs{i}(1);
    min_thres = (rrs{i}(2)-rrs{i}(1))*(1-w1)+rrs{i}(1);
    ind2 = ind1(feat(2,ind1)+feat(3,ind1)>=max_thres & feat(2,ind1)<min_thres);
    tmp_im = im1.*(ismember(im2_a,[0 ind2]));    
    % remove cropped part
    bid = ind2(feat(2,ind2)<rrs{i}(1) | feat(2,ind2)+feat(3,ind2)>rrs{i}(2));
    for j=bid
        tmp = im2_a==j;
        tmp(1:rrs{i}(1)-1,:) = 0;
        tmp(rrs{i}(2)+1:end,:) = 0;
        [id1,num]=bwlabel(tmp);
        if num>1
            [~,cid] = max(histc(id1(:),1:num));
            tmp_im = tmp_im.*ismember(id1,[0,cid]);
        else
            % not two liners
            if feat(3,j)<2.4*char_h
                tmp_im(im2_a==j) = 0;
            end
        end
    end
    tmp_im = tmp_im(rrs{i}(1):rrs{i}(2),ccs{i}(1):ccs{i}(2));
    
    % fine seg: 
    [im3_a,im3_b]=bwlabel(tmp_im);
    feat3 = U_stat(im3_a,im3_b);
    % remove dot: small and below
    dot = find(max(feat3(3:4,:))<char_h*0.6);
    tmp_im = tmp_im.*(~ismember(im3_a,dot(feat3(2,dot)>(1-w1)*size(im3_a,1))));
    
    % tighten up
    rowsum = sum(tmp_im,2);
    colsum = sum(tmp_im);
    tmp_im = tmp_im(find(rowsum~=0,1,'first'):find(rowsum~=0,1,'last'),find(colsum~=0,1,'first'):find(colsum~=0,1,'last'));    
    imwrite(1-tmp_im,[nn(1:end-4) '_line' num2str(cc) '.jpg']);
    imwrite(1-im1(rrs{i}(1):rrs{i}(2)),[nn(1:end-4) '_rline' num2str(cc) '.jpg']);
    cc = cc+1;
end


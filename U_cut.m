function U_cut(im,opt)
% vertical cut
im2 = im;
se = strel('disk',1);
im3 = imerode(im2,se);
switch opt
    % cut into 2
    case 1
        num_c =1;
        while num_c==1
            im2 = imerode(im2,se);
            [ind,num_c] = bwlabel(im2);
        end        
    case 2
        % cut max smaller
        num=nnz(im);
        num_c = num;
        while num_c> num*0.6
            [ind,num_c] = bwlabel(im2);
            num_c = max(histc(ind,1:num_c));
        end
end

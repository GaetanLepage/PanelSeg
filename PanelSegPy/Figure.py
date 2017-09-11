import xml.etree.ElementTree as ET

import cv2
from os.path import join, split
from Panel import Panel


class Figure:
    """
    A class for a Figure
    image_path is the path to the figure image file
    id is the unique id to each figure
    image_orig is the original color image
    image is the extended (50 pixels in all directions) image
    image_gray is the grayscale image (extended)
    panels contain all panels
    """

    PADDING = 50

    def __init__(self, image_path):
        self.image_path = image_path
        path, file = split(image_path)
        path, folder = split(path)
        self.id = "{0}-{1}".format(folder, file)

        self.image_orig = None
        self.image = None
        self.image_gray = None

        self.panels = None

    def load_image(self):
        """
        Load the original image, and then padding the image and convert to gray scale
        :return:
        """
        self.image_orig = cv2.imread(self.image_path, cv2.IMREAD_COLOR)
        self.image = cv2.copyMakeBorder(self.image_orig, self.PADDING, self.PADDING, self.PADDING, self.PADDING,
                                        cv2.BORDER_CONSTANT, 0)
        self.image_gray = cv2.cvtColor(self.image, cv2.COLOR_BGR2GRAY)
        pass

    def crop_patch(self, roi, is_gray=True):
        x, y, w, h = roi[0] + self.PADDING, roi[1] + self.PADDING, roi[2], roi[3]
        if is_gray:
            patch = self.image_gray[y:y+h, x:x+w]
        else:
            patch = self.image[y:y+h, x:x+w]
        return patch

    def crop_label_patches(self, is_gray=True):
        for panel in self.panels:
            panel.label_rects = []
            panel.label_patches = []

            panel.label_rects.append(panel.label_rect);
            patch = self.crop_patch(panel.label_rect, is_gray)
            panel.label_patches.append(patch)

    def save_label_patches(self, target_folder):
        for panel in self.panels:
            for i in range(0, len(panel.label_patches)):
                rect = panel.label_rects[i]
                patch = panel.label_patches[i]
                patch_file = self.id + "_".join(str(x) for x in rect) + ".png"
                patch_file = join(target_folder, panel.label, patch_file)
                cv2.imwrite(patch_file, patch)


    def load_gt_annotation(self,
                           which_annotation='label',  # label: label annotation only
                           ):
        """
        Load Ground Truth annotation
        :param which_annotation: 'label' load label annotation only; 'panel_and_label' load both panel and label annotation
        :return: None, the loaded annotation is saved to self.panels
        """
        iphotodraw_path = self.image_path.replace('.jpg', '_data.xml')
        self.load_annotation(iphotodraw_path, which_annotation)

    def load_annotation(self,
                        annotation_file_path,
                        which_annotation='label',  # label: label annotation only
                        file_type='iphotodraw'
                        ):
        """
        :param annotation_file_path: the file path to the annotation
        :param which_annotation: 'label' load label annotation only; 'panel_and_label' load both panel and label annotation
        :param file_type: 'iphotodraw' annotation file is in iphotodraw format
        :return: None, the loaded annotation is saved to self.panels
        """
        if which_annotation == 'label':
            if file_type == 'iphotodraw':
                self._load_annotation_label_iphotodraw(annotation_file_path)
            else:
                raise Exception(file_type + ' is unknown!')

        elif which_annotation == 'panel_and_label':
            if file_type == 'iphotodraw':
                self._load_annotation_panel_and_label_iphotodraw(annotation_file_path)
            else:
                raise Exception(file_type + ' is unknown!')

        else:
            raise Exception(which_annotation + ' is unknown!')

    def _load_annotation_label_iphotodraw(self,
                                          iphotodraw_path):
        """
        Load label annotation from iphotodraw formatted file
        :param iphotodraw_path:
        :return:
        """
        # create element tree object
        tree = ET.parse(iphotodraw_path)
        # get root element
        root = tree.getroot()

        shape_items = root.findall('./Layers/Layer/Shapes/Shape')

        # Read All Label Items
        label_items = []
        for shape_item in shape_items:
            text_item = shape_item.find('./BlockText/Text')
            text = text_item.text.lower()
            if text.startswith('label'):
                label_items.append(shape_item)

        # Form individual panels
        panels = []
        for label_item in label_items:
            text_item = label_item.find('./BlockText/Text')
            label = text_item.text
            words = label.split(' ')
            if len(words) is not 2:
                raise Exception(iphotodraw_path + ' ' + label + ' panel is not correct')
            label = words[1]

            extent_item = label_item.find('./Data/Extent')
            height = extent_item.get('Height')
            width = extent_item.get('Width')
            x = extent_item.get('X')
            y = extent_item.get('Y')
            label_rect = [int(x), int(y), int(width), int(height)]

            panel = Panel(label, None, label_rect)
            panels.append(panel)

        self.panels = panels

    def _load_annotation_panel_and_label_iphotodraw(self,
                                                    iphotodraw_path):
        """
        Load both panel and label annotation from iphotodraw formatted file
        :param iphotodraw_path:
        :return:
        """
        # create element tree object
        tree = ET.parse(iphotodraw_path)
        # get root element
        root = tree.getroot()

        shape_items = root.findall('./Layers/Layer/Shapes/Shape')

        # Read All Items (Panels and Labels)
        panel_items = []
        label_items = []
        for shape_item in shape_items:
            text_item = shape_item.find('./BlockText/Text')
            text = text_item.text.lower()
            if text.startswith('panel'):
                panel_items.append(shape_item)
            elif text.startswith('label'):
                label_items.append(shape_item)

        # Form individual panels
        panels = []
        for panel_item in panel_items:
            text_item = panel_item.find('./BlockText/Text')
            label = text_item.text
            words = label.split(' ')
            if len(words) is not 2:
                raise Exception(iphotodraw_path + ' ' + label + ' panel is not correct')
            label = words[1]

            extent_item = panel_item.find('./Data/Extent')
            height = extent_item.get('Height')
            width = extent_item.get('Width')
            x = extent_item.get('X')
            y = extent_item.get('Y')
            panel_rect = [int(x), int(y), int(width), int(height)]

            panel = Panel(label, panel_rect, None)
            panels.append(panel)

        # Fill in label rects
        for panel in panels:
            for label_item in label_items:
                text_item = label_item.find('./BlockText/Text')
                label = text_item.text
                words = label.split(' ')
                if len(words) is not 2:
                    raise Exception(iphotodraw_path + ' ' + label + ' label is not correct')

                label = words[1]
                if label.lower() == panel.label.lower():
                    extent_item = label_item.find('./Data/Extent')
                    height = extent_item.get('Height')
                    width = extent_item.get('Width')
                    x = extent_item.get('X')
                    y = extent_item.get('Y')

                    label_rect = [int(x), int(y), int(width), int(height)]
                    panel.label_rect = label_rect

        self.panels = panels


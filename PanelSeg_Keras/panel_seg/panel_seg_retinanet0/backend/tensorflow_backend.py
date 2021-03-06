"""
Copyright 2017-2018 Fizyr (https://fizyr.com)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import tensorflow as tf


def map_fn(*args, **kwargs):
    """ See https://www.tensorflow.org/versions/master/api_docs/python/tf/map_fn .
    """
    return tf.map_fn(*args, **kwargs)


def pad(*args, **kwargs):
    """ See https://www.tensorflow.org/versions/master/api_docs/python/tf/pad .
    """
    return tf.pad(*args, **kwargs)


def top_k(*args, **kwargs):
    """ See https://www.tensorflow.org/versions/master/api_docs/python/tf/nn/top_k .
    """
    return tf.nn.top_k(*args, **kwargs)


def clip_by_value(*args, **kwargs):
    """ See https://www.tensorflow.org/versions/master/api_docs/python/tf/clip_by_value .
    """
    return tf.clip_by_value(*args, **kwargs)


def resize_images(images, size, method='bilinear', align_corners=False):
    """ See https://www.tensorflow.org/versions/master/api_docs/python/tf/image/resize_images .

    Args
        method: The method used for interpolation. One of ('bilinear', 'nearest', 'bicubic', 'area').
    """
    methods = {
        'bilinear': tf.image.ResizeMethod.BILINEAR,
        'nearest' : tf.image.ResizeMethod.NEAREST_NEIGHBOR,
        'bicubic' : tf.image.ResizeMethod.BICUBIC,
        'area'    : tf.image.ResizeMethod.AREA,
    }
    return tf.image.resize_images(images, size, methods[method], align_corners)


def non_max_suppression(*args, **kwargs):
    """ See https://www.tensorflow.org/versions/master/api_docs/python/tf/image/non_max_suppression .
    """
    return tf.image.non_max_suppression(*args, **kwargs)


def range(*args, **kwargs):
    """ See https://www.tensorflow.org/versions/master/api_docs/python/tf/range .
    """
    return tf.range(*args, **kwargs)


def scatter_nd(*args, **kwargs):
    """ See https://www.tensorflow.org/versions/master/api_docs/python/tf/scatter_nd .
    """
    return tf.scatter_nd(*args, **kwargs)


def gather_nd(*args, **kwargs):
    """ See https://www.tensorflow.org/versions/master/api_docs/python/tf/gather_nd .
    """
    return tf.gather_nd(*args, **kwargs)


def meshgrid(*args, **kwargs):
    """ See https://www.tensorflow.org/versions/master/api_docs/python/tf/meshgrid .
    """
    return tf.meshgrid(*args, **kwargs)


def where(*args, **kwargs):
    """ See https://www.tensorflow.org/versions/master/api_docs/python/tf/where .
    """
    return tf.where(*args, **kwargs)


def logical_and(*args, **kwargs):
    return tf.logical_and(*args, **kwargs)


def unstack(*args, **kwargs):
    return tf.unstack(*args, **kwargs)


def reduce_max(*args, **kwargs):
    return tf.reduce_max(*args, **kwargs)


def while_loop(*args, **kwargs):
    return tf.while_loop(*args, **kwargs)


def add(*args, **kwargs):
    return tf.add(*args, **kwargs)


def get_variable(*args, **kwargs):
    return tf.get_variable(*args, **kwargs)


def create_tensorarray(dtype, size):
    return tf.TensorArray(dtype=dtype, size=size)

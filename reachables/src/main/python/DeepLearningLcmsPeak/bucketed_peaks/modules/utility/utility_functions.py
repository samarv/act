"""
"                                                                        "
"  This file is part of the 20n/act project.                             "
"  20n/act enables DNA prediction for synthetic biology/bioengineering.  "
"  Copyright (C) 2017 20n Labs, Inc.                                     "
"                                                                        "
"  Please direct all queries to act@20n.com.                             "
"                                                                        "
"  This program is free software: you can redistribute it and/or modify  "
"  it under the terms of the GNU General Public License as published by  "
"  the Free Software Foundation, either version 3 of the License, or     "
"  (at your option) any later version.                                   "
"                                                                        "
"  This program is distributed in the hope that it will be useful,       "
"  but WITHOUT ANY WARRANTY; without even the implied warranty of        "
"  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         "
"  GNU General Public License for more details.                          "
"                                                                        "
"  You should have received a copy of the GNU General Public License     "
"  along with this program.  If not, see <http://www.gnu.org/licenses/>. "
"                                                                        "
"""

from __future__ import absolute_import, division, print_function

import json
import math
import os
import pickle
import sys


def row_to_mz(row, mz_division, min_mz):
    return row * mz_division + min_mz - mz_division * 0.5


# Fill numpy array with appropriate values
def assign_row_by_mz(mz, mz_split, mz_min):
    mz_row_assignment = float(mz - mz_min) / mz_split
    if mz_row_assignment < 0:
        raise RuntimeError("M/Z assignment was less than 0. M/Z "
                           "value supplied was {}, granularity was {}, "
                           "min M/Z value was {}".format(mz, mz_split, mz_min))
    return int(round(mz_row_assignment))


def assign_column_by_time(time, time_step, time_min):
    time_column = float(time - time_min) / time_step
    if time_column < 0:
        raise RuntimeError("Time column is less than 0.  "
                           "Time value supplied was {}, granularity was {}, "
                           "min time was {}".format(time, time_step, time_min))
    return int(math.floor(time_column))


def column_number_to_time(colum_number, time_step, time_min):
    return colum_number * time_step + time_min


def parse_lcms_scan_file_name(lcms_scan_name):
    return lcms_scan_name.split(".nc")[0]


def output_analysis_summary(output_directory, output_descriptor, summary_dictionary):
    # Write run summary information
    with open(os.path.join(output_directory, "{}_run_summary.json".format(output_descriptor)), "w") as f:
        json.dump(summary_dictionary, f, indent=4, sort_keys=True)


def save_model(model_directory, model_name, model):
    model_location = os.path.join(model_directory, model_name + ".model")

    with open(model_location, "w") as f:
        # Complex objects require more recursive steps to pickle.
        previous_limit = sys.getrecursionlimit()
        sys.setrecursionlimit(10000)
        pickle.dump(model, f)
        sys.setrecursionlimit(previous_limit)

    return model_location


def load_previous_model(model_location, output_directory):
    print("Using previously created model at {}".format(model_location))
    with open(model_location, "rb") as f:
        autoencoder = pickle.load(f)
        autoencoder.set_output_directory(output_directory)
        return autoencoder
